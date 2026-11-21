import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {
  Alert,
  Animated,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import PixelButton from '../components/PixelButton';
import PixelFrame from '../components/PixelFrame';
import PlayingCard from '../components/PlayingCard';
import StepButton from '../components/StepButton';
import {BridgeError, nativeBridge} from '../nativeBridge';
import {
  canGamble,
  clampStake,
  HAND_LABELS,
  MAX_STAKE_MINUTES,
  maxStakeMinutes,
  MIN_STAKE_MINUTES,
  netSeconds,
  newGameId,
  STAKE_STEP_MINUTES,
} from '../teenPatti';
import {RootStackParamList, TeenPattiResult} from '../types';
import {formatClock, formatDuration} from '../time';
import {fonts, palette} from '../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'TeenPatti'>;

/** betting → dealing (result already saved in Kotlin, cards animating) → result. */
type Phase = 'betting' | 'dealing' | 'result';

const QUICK_STAKES = [5, 15, 30, 60];
const newAnims = () => Array.from({length: 3}, () => new Animated.Value(0));

export default function TeenPatti({navigation, route}: Props) {
  const {app} = route.params;
  const [remainingSeconds, setRemainingSeconds] = useState(app.remainingSeconds);
  const [stake, setStake] = useState(() => clampStake(15, app.remainingSeconds));
  const [allIn, setAllIn] = useState(false);
  const [phase, setPhase] = useState<Phase>('betting');
  const [result, setResult] = useState<TeenPattiResult | null>(null);

  // Reused on retry so a call that reached Kotlin is never played (or paid) twice.
  const pendingGameId = useRef<string | null>(null);

  const playerDealt = useMemo(newAnims, []);
  const houseDealt = useMemo(newAnims, []);
  const playerFlip = useMemo(newAnims, []);
  const houseFlip = useMemo(newAnims, []);

  const refresh = useCallback(() => {
    nativeBridge
      .getSnapshot()
      .then(s => {
        const latest = s.apps.find(a => a.packageName === app.packageName);
        if (latest) {
          setRemainingSeconds(latest.remainingSeconds);
        }
      })
      .catch(() => {});
  }, [app.packageName]);

  useFocusEffect(refresh);

  // Keep the bet legal as the balance changes.
  useEffect(() => {
    setStake(s => clampStake(s, remainingSeconds));
  }, [remainingSeconds]);

  const playable = canGamble(remainingSeconds);
  const busy = phase === 'dealing';
  const maxStake = maxStakeMinutes(remainingSeconds);
  // All-in bets the exact balance, up to the 6h maximum bet.
  const stakeLabel = allIn
    ? formatClock(Math.min(remainingSeconds, MAX_STAKE_MINUTES * 60))
    : formatDuration(stake * 60_000);

  const resetTable = () => {
    [...playerDealt, ...houseDealt, ...playerFlip, ...houseFlip].forEach(v => v.setValue(0));
  };

  const animateRound = (done: () => void) => {
    const timing = (v: Animated.Value, duration: number) =>
      Animated.timing(v, {toValue: 1, duration, useNativeDriver: true});
    // Deal alternately, player first, like at a real table.
    const deal = [0, 1, 2].flatMap(i => [timing(playerDealt[i], 200), timing(houseDealt[i], 200)]);
    Animated.sequence([
      Animated.stagger(110, deal),
      Animated.delay(250),
      Animated.stagger(160, playerFlip.map(v => timing(v, 260))),
      Animated.delay(550),
      Animated.stagger(320, houseFlip.map(v => timing(v, 260))),
    ]).start(() => done());
  };

  const deal = async () => {
    if (busy || !playable) {
      return;
    }
    const gameId = pendingGameId.current ?? newGameId();
    pendingGameId.current = gameId;
    resetTable();
    setResult(null);
    setPhase('dealing');

    try {
      const played = await nativeBridge.playTeenPatti(gameId, app.packageName, stake, allIn);
      pendingGameId.current = null;
      setResult(played);
      animateRound(() => {
        setRemainingSeconds(played.balanceAfterSeconds);
        setAllIn(false);
        setPhase('result');
      });
    } catch (e) {
      const error = e as BridgeError;
      // A rejected bet never reached the table, so the id is free to drop.
      if (error.code?.startsWith('E_STAKE_') || error.code === 'E_NOT_PROTECTED') {
        pendingGameId.current = null;
      }
      setPhase('betting');
      Alert.alert("Couldn't deal", error.message ?? String(e));
      refresh();
    }
  };

  const adjust = useCallback(
    (delta: number) => {
      setAllIn(false);
      setStake(s => clampStake(s + delta, remainingSeconds));
    },
    [remainingSeconds],
  );
  const stepDown = useCallback(() => adjust(-STAKE_STEP_MINUTES), [adjust]);
  const stepUp = useCallback(() => adjust(STAKE_STEP_MINUTES), [adjust]);

  const winner = phase === 'result' ? result?.outcome : undefined;

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.topBar}>
        <Pressable onPress={() => navigation.goBack()} disabled={busy} hitSlop={12}>
          <Text style={[styles.back, busy && styles.dim]}>{'< BACK'}</Text>
        </Pressable>
        <PixelFrame color={palette.panel} contentStyle={styles.balancePill}>
          <Text style={styles.balanceLabel} numberOfLines={1}>
            {app.appName}
          </Text>
          <Text style={styles.balanceValue}>{formatClock(remainingSeconds)}</Text>
        </PixelFrame>
      </View>

      <View style={styles.content}>
        <PixelFrame shadow color="#0f3b33" borderColor={palette.ink} bevel style={styles.table} contentStyle={styles.tableContent}>
          <HandRow
            label="HOUSE"
            hand={result && phase === 'result' ? HAND_LABELS[result.house.hand] : undefined}
            cards={result?.house.cards}
            dealt={houseDealt}
            flip={houseFlip}
            highlight={winner === 'HOUSE_WIN'}
          />

          <View style={styles.banner}>
            {phase === 'result' && result ? (
              <ResultBanner result={result} />
            ) : (
              <Text style={styles.bannerIdle}>{busy ? 'DEALING...' : 'PLACE YOUR BET'}</Text>
            )}
          </View>

          <HandRow
            label="YOU"
            hand={result && phase === 'result' ? HAND_LABELS[result.player.hand] : undefined}
            cards={result?.player.cards}
            dealt={playerDealt}
            flip={playerFlip}
            highlight={winner === 'PLAYER_WIN'}
          />
        </PixelFrame>

        {playable ? (
          <PixelFrame shadow style={styles.betPanel} contentStyle={styles.betContent}>
            <Text style={styles.betLabel}>{allIn ? 'ALL IN' : 'BET'}</Text>
            <View style={styles.stakeRow}>
              <StepButton
                label="-"
                size={48}
                onStep={stepDown}
                disabled={busy || (!allIn && stake <= MIN_STAKE_MINUTES)}
              />
              <Text style={styles.stakeValue}>{stakeLabel}</Text>
              <StepButton
                label="+"
                size={48}
                onStep={stepUp}
                disabled={busy || (!allIn && stake >= maxStake)}
              />
            </View>
            <View style={styles.chipRow}>
              {QUICK_STAKES.filter(m => m <= maxStake).map(m => (
                <Chip
                  key={m}
                  label={formatDuration(m * 60_000)}
                  active={!allIn && stake === m}
                  disabled={busy}
                  onPress={() => {
                    setAllIn(false);
                    setStake(m);
                  }}
                />
              ))}
              <Chip label="ALL IN" active={allIn} danger disabled={busy} onPress={() => setAllIn(a => !a)} />
            </View>
            <Text style={styles.rules}>
              Win doubles your bet · lose it all · tie returns it
            </Text>
          </PixelFrame>
        ) : (
          <PixelFrame shadow style={styles.betPanel} contentStyle={styles.betContent}>
            <Text style={styles.brokeText}>
              You need at least {MIN_STAKE_MINUTES} minutes left on {app.appName} to gamble.
            </Text>
          </PixelFrame>
        )}
      </View>

      <View style={styles.footer}>
        <PixelButton
          label={busy ? 'Dealing...' : phase === 'result' ? `Deal again · ${stakeLabel}` : `Deal · ${stakeLabel}`}
          onPress={deal}
          disabled={busy || !playable}
          color={palette.accent3}
        />
      </View>
    </SafeAreaView>
  );
}

function HandRow(props: {
  label: string;
  hand?: string;
  cards?: TeenPattiResult['player']['cards'];
  dealt: Animated.Value[];
  flip: Animated.Value[];
  highlight: boolean;
}) {
  return (
    <View style={styles.handRow}>
      <View style={styles.handHeader}>
        <Text style={styles.handLabel}>{props.label}</Text>
        {props.hand && (
          <Text style={[styles.handName, props.highlight && {color: palette.accent3}]}>{props.hand}</Text>
        )}
      </View>
      <View style={styles.cards}>
        {props.dealt.map((d, i) => (
          <PlayingCard
            key={i}
            card={props.cards?.[i]}
            dealt={d}
            flip={props.flip[i]}
            highlight={props.highlight}
          />
        ))}
      </View>
    </View>
  );
}

function ResultBanner({result}: {result: TeenPattiResult}) {
  const net = netSeconds(result);
  if (result.outcome === 'TIE') {
    return <Text style={[styles.bannerText, {color: palette.text}]}>PUSH · bet returned</Text>;
  }
  const won = result.outcome === 'PLAYER_WIN';
  const capped = won && net < result.stakeSeconds;
  return (
    <View style={styles.bannerCenter}>
      <Text style={[styles.bannerText, {color: won ? palette.success : palette.danger}]}>
        {won ? 'YOU WIN' : 'HOUSE WINS'} {won ? '+' : '-'}
        {formatDuration(Math.abs(net) * 1000)}
      </Text>
      {capped && <Text style={styles.bannerNote}>Balance capped at 24h</Text>}
    </View>
  );
}

function Chip(props: {label: string; active: boolean; danger?: boolean; disabled: boolean; onPress: () => void}) {
  const activeColor = props.danger ? palette.danger : palette.accent2;
  return (
    <PixelButton
      small
      label={props.label}
      color={props.active ? activeColor : palette.panelAlt}
      textColor={props.active ? '#fff' : palette.text}
      disabled={props.disabled}
      onPress={props.onPress}
      style={styles.chip}
    />
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: palette.background},
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 14,
    paddingTop: 8,
  },
  back: {color: palette.accent2, fontFamily: fonts.pixel, fontSize: 10},
  dim: {opacity: 0.4},
  balancePill: {alignItems: 'flex-end', paddingHorizontal: 12, paddingVertical: 8, maxWidth: 200},
  balanceLabel: {color: palette.muted, fontFamily: fonts.body, fontSize: 17},
  balanceValue: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 14, lineHeight: 20, marginTop: 2},
  content: {flex: 1, justifyContent: 'center', paddingHorizontal: 14, paddingVertical: 4},
  table: {marginTop: 4},
  tableContent: {padding: 10},
  handRow: {alignItems: 'center'},
  handHeader: {flexDirection: 'row', alignItems: 'baseline', marginBottom: 8},
  handLabel: {color: '#cfe8df', fontFamily: fonts.pixel, fontSize: 9},
  handName: {color: palette.text, fontFamily: fonts.body, fontSize: 20, marginLeft: 8},
  cards: {flexDirection: 'row', justifyContent: 'center'},
  banner: {minHeight: 40, alignItems: 'center', justifyContent: 'center', marginVertical: 4},
  bannerCenter: {alignItems: 'center'},
  bannerIdle: {color: '#cfe8df', fontFamily: fonts.pixel, fontSize: 10},
  bannerText: {fontFamily: fonts.pixel, fontSize: 14, lineHeight: 22, textAlign: 'center'},
  bannerNote: {color: palette.muted, fontFamily: fonts.body, fontSize: 18, marginTop: 2},
  betPanel: {marginTop: 8},
  betContent: {padding: 10, alignItems: 'center'},
  betLabel: {color: palette.muted, fontFamily: fonts.pixel, fontSize: 9},
  stakeRow: {flexDirection: 'row', alignItems: 'center', marginTop: 4},
  stakeValue: {
    color: palette.accent3,
    fontFamily: fonts.pixel,
    fontSize: 20,
    lineHeight: 28,
    minWidth: 150,
    textAlign: 'center',
  },
  chipRow: {flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'center', marginTop: 4},
  chip: {margin: 2},
  rules: {color: palette.muted, fontFamily: fonts.body, fontSize: 18, marginTop: 4, textAlign: 'center'},
  brokeText: {color: palette.text, fontFamily: fonts.body, fontSize: 21, textAlign: 'center'},
  footer: {padding: 14, paddingTop: 4},
});
