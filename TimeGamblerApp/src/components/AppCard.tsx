import React from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';
import PixelButton from './PixelButton';
import PixelFrame from './PixelFrame';
import {ProtectedApp} from '../types';
import {fonts, palette, PIXEL} from '../theme';
import {formatClock} from '../time';
import {canGamble} from '../teenPatti';

type Props = {app: ProtectedApp; onPress?: () => void; onGamble?: () => void};

/** Number of blocks in the time-left bar. */
const BAR_SEGMENTS = 20;

export default function AppCard({app, onPress, onGamble}: Props) {
  const limitSeconds = Math.max(1, app.dailyLimitMinutes * 60);
  const progress = Math.min(1, Math.max(0, app.remainingSeconds / limitSeconds));
  const exhausted = app.remainingSeconds <= 0;
  const filled = Math.ceil(progress * BAR_SEGMENTS);
  const barColor = progress < 0.2 ? palette.danger : palette.accent3;

  return (
    <Pressable onPress={onPress}>
      {({pressed}) => (
        <PixelFrame
          shadow
          color={pressed ? palette.panelAlt : palette.panel}
          borderColor={exhausted ? palette.danger : palette.ink}
          style={styles.card}
          contentStyle={styles.content}>
          <View style={styles.headerRow}>
            <PixelFrame color={palette.accent2} bevel style={styles.iconBadge} contentStyle={styles.iconContent}>
              <Text style={styles.iconText}>{app.appName.charAt(0).toUpperCase()}</Text>
            </PixelFrame>
            <View style={styles.titleBlock}>
              <Text style={styles.title} numberOfLines={1}>
                {app.appName}
              </Text>
              <Text style={styles.subtitle}>Daily limit {app.dailyLimitMinutes} min</Text>
            </View>
            {exhausted && <Text style={styles.outTag}>OUT!</Text>}
          </View>

          <Text style={[styles.timerText, exhausted && {color: palette.danger}]}>
            {formatClock(app.remainingSeconds)}
          </Text>
          <PixelFrame color="#202838" sunken bevel contentStyle={styles.barFrame}>
            <View style={styles.bar}>
              {Array.from({length: BAR_SEGMENTS}, (_, i) => (
                <View key={i} style={[styles.segment, i < filled && {backgroundColor: barColor}]} />
              ))}
            </View>
          </PixelFrame>

          {onGamble && (
            <PixelButton
              small
              label={canGamble(app.remainingSeconds) ? '♠ Gamble this time' : 'Need 5 min to gamble'}
              color={palette.accent3}
              disabled={!canGamble(app.remainingSeconds)}
              onPress={onGamble}
              style={styles.gamble}
            />
          )}
        </PixelFrame>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {marginBottom: 14},
  content: {padding: 16},
  headerRow: {flexDirection: 'row', alignItems: 'center'},
  iconBadge: {width: 44, height: 44, marginRight: 12},
  iconContent: {flex: 1, padding: 0, alignItems: 'center', justifyContent: 'center'},
  iconText: {color: '#fff', fontFamily: fonts.pixel, fontSize: 16, lineHeight: 20},
  titleBlock: {flex: 1},
  title: {color: palette.text, fontFamily: fonts.pixel, fontSize: 12, lineHeight: 18},
  subtitle: {color: palette.muted, fontFamily: fonts.body, fontSize: 18, marginTop: 2},
  outTag: {color: palette.danger, fontFamily: fonts.pixel, fontSize: 10},
  timerText: {
    color: palette.text,
    fontFamily: fonts.pixel,
    fontSize: 28,
    lineHeight: 36,
    textAlign: 'center',
    marginTop: 14,
    marginBottom: 10,
  },
  barFrame: {padding: PIXEL * 2},
  bar: {flexDirection: 'row', height: PIXEL * 3},
  segment: {flex: 1, marginHorizontal: 1, backgroundColor: '#2a3447'},
  gamble: {marginTop: 14},
});
