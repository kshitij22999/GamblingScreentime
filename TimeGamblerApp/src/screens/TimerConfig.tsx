import React, {useCallback, useState} from 'react';
import {SafeAreaView, StyleSheet, Text, View} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import LimitConfirmDialog, {LimitAction} from '../components/LimitConfirmDialog';
import PixelButton from '../components/PixelButton';
import PixelFrame from '../components/PixelFrame';
import PixelMeter from '../components/PixelMeter';
import StepButton from '../components/StepButton';
import {BridgeError, nativeBridge} from '../nativeBridge';
import {LIMIT_STEP_MINUTES, MAX_LIMIT_MINUTES, MIN_LIMIT_MINUTES, clampLimit} from '../limits';
import {RootStackParamList} from '../types';
import {fonts, palette} from '../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'TimerConfig'>;

const PRESETS = [15, 30, 45, 60, 90, 120];

export default function TimerConfig({route, navigation}: Props) {
  const {app, existing} = route.params;
  const [minutes, setMinutes] = useState(existing?.dailyLimitMinutes ?? 30);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // The action stays set while the dialog fades out, so only `confirming` toggles visibility.
  const [action, setAction] = useState<LimitAction>({kind: 'cancel'});
  const [confirming, setConfirming] = useState(false);

  const unchanged = existing?.dailyLimitMinutes === minutes;

  const ask = (next: LimitAction) => {
    setAction(next);
    setConfirming(true);
  };

  const run = async (work: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await work();
      setConfirming(false);
      navigation.navigate('Dashboard');
    } catch (e) {
      setConfirming(false);
      setError((e as BridgeError).message);
    } finally {
      setBusy(false);
    }
  };

  const save = () => run(() => nativeBridge.setAppLimit(app, minutes));
  const cancelLimit = () => run(() => nativeBridge.cancelAppLimit(app.packageName));

  const stepDown = useCallback(() => setMinutes(m => clampLimit(m - LIMIT_STEP_MINUTES)), []);
  const stepUp = useCallback(() => setMinutes(m => clampLimit(m + LIMIT_STEP_MINUTES)), []);
  const fromMeter = useCallback((segment: number) => setMinutes(clampLimit(segment * LIMIT_STEP_MINUTES)), []);

  return (
    <SafeAreaView style={styles.screen}>
      <Text style={styles.title} numberOfLines={2}>
        {app.appName.toUpperCase()}
      </Text>
      <Text style={styles.subtitle}>{existing ? 'Daily limit' : 'Choose a daily limit'}</Text>

      <PixelFrame shadow style={styles.stepper} contentStyle={styles.stepperContent}>
        <View style={styles.stepperRow}>
          <StepButton label="-" onStep={stepDown} disabled={minutes <= MIN_LIMIT_MINUTES} />
          <View style={styles.valueBlock}>
            <Text style={styles.value}>{minutes}</Text>
            <Text style={styles.valueUnit}>min / day</Text>
          </View>
          <StepButton label="+" onStep={stepUp} disabled={minutes >= MAX_LIMIT_MINUTES} />
        </View>
        <PixelMeter
          value={minutes / LIMIT_STEP_MINUTES}
          segments={MAX_LIMIT_MINUTES / LIMIT_STEP_MINUTES}
          onChange={fromMeter}
        />
        <Text style={styles.meterHint}>Hold +/- or drag the bar</Text>
      </PixelFrame>

      <View style={styles.presetGrid}>
        {PRESETS.map(p => (
          <PixelButton
            key={p}
            small
            label={`${p}m`}
            color={minutes === p ? palette.accent3 : palette.panelAlt}
            textColor={minutes === p ? '#1a1d22' : palette.text}
            onPress={() => setMinutes(p)}
            style={styles.preset}
          />
        ))}
      </View>

      <Text style={styles.rule}>
        You can change or cancel this limit whenever you like. Changing it keeps the time you've already used today.
      </Text>

      {error && <Text style={styles.error}>{error}</Text>}

      <View style={styles.footer}>
        {existing && (
          <PixelButton
            label="Cancel this limit"
            color={palette.panelAlt}
            textColor={palette.danger}
            disabled={busy}
            onPress={() => ask({kind: 'cancel'})}
            style={styles.cancelButton}
          />
        )}
        <PixelButton
          label={busy ? 'Saving...' : existing ? 'Change limit' : 'Start protecting'}
          disabled={busy || unchanged}
          onPress={() =>
            existing
              ? ask({kind: 'change', fromMinutes: existing.dailyLimitMinutes, toMinutes: minutes})
              : save()
          }
        />
      </View>

      {existing && (
        <LimitConfirmDialog
          visible={confirming}
          appName={app.appName}
          action={action}
          busy={busy}
          onKeep={() => setConfirming(false)}
          onConfirm={action.kind === 'cancel' ? cancelLimit : save}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: palette.background, padding: 20},
  title: {color: palette.text, fontFamily: fonts.pixel, fontSize: 20, lineHeight: 30, marginTop: 18},
  subtitle: {color: palette.muted, fontFamily: fonts.body, fontSize: 22, marginTop: 4},
  stepper: {marginTop: 24},
  stepperContent: {padding: 16},
  stepperRow: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16},
  valueBlock: {alignItems: 'center'},
  value: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 40, lineHeight: 48, minWidth: 130, textAlign: 'center'},
  valueUnit: {color: palette.muted, fontFamily: fonts.pixel, fontSize: 8, marginTop: 6},
  meterHint: {color: palette.muted, fontFamily: fonts.body, fontSize: 18, textAlign: 'center', marginTop: 8},
  presetGrid: {flexDirection: 'row', flexWrap: 'wrap', marginTop: 18},
  preset: {marginRight: 8, marginBottom: 10},
  rule: {color: palette.muted, fontFamily: fonts.body, fontSize: 19, lineHeight: 21, marginTop: 6},
  error: {color: palette.danger, fontFamily: fonts.body, fontSize: 20, marginTop: 16},
  footer: {marginTop: 'auto'},
  cancelButton: {marginBottom: 12},
});
