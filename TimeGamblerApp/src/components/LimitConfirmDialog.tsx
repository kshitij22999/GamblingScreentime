import React from 'react';
import {Modal, StyleSheet, Text, View} from 'react-native';
import PixelButton from './PixelButton';
import PixelFrame from './PixelFrame';
import {fonts, palette} from '../theme';

/** What the user is about to do to an existing limit. */
export type LimitAction =
  | {kind: 'cancel'}
  | {kind: 'change'; fromMinutes: number; toMinutes: number};

type Props = {
  visible: boolean;
  appName: string;
  action: LimitAction;
  busy: boolean;
  onKeep: () => void;
  onConfirm: () => void;
};

export default function LimitConfirmDialog({visible, appName, action, busy, onKeep, onConfirm}: Props) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onKeep}>
      <View style={styles.backdrop}>
        <PixelFrame
          shadow
          color={palette.panel}
          borderColor={action.kind === 'cancel' ? palette.accent : palette.accent3}
          contentStyle={styles.card}>
          <Text style={styles.suit}>♠ ♥ ♣ ♦</Text>
          <Text style={styles.headline}>Fold your hand?</Text>
          <Text style={styles.body}>{appName} is shuffling the deck and grinning. The house always wins, you know.</Text>
          <Text style={styles.summary}>
            {action.kind === 'cancel'
              ? `Removes the limit on ${appName} completely.`
              : `${action.fromMinutes} min → ${action.toMinutes} min a day`}
          </Text>
          <PixelButton label="Stay at the table" onPress={onKeep} disabled={busy} />
          <PixelButton
            label={busy ? 'Shuffling...' : action.kind === 'cancel' ? 'Fold. Set me free' : 'Fold. Change it'}
            color={palette.panelAlt}
            textColor={action.kind === 'cancel' ? palette.danger : palette.text}
            onPress={onConfirm}
            disabled={busy}
            style={styles.confirm}
          />
        </PixelFrame>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(5,5,10,0.82)',
    justifyContent: 'center',
    padding: 24,
  },
  card: {padding: 22},
  suit: {color: palette.accent, textAlign: 'center', letterSpacing: 8, fontSize: 16},
  headline: {color: palette.text, fontFamily: fonts.pixel, fontSize: 16, lineHeight: 24, textAlign: 'center', marginTop: 12},
  body: {color: palette.text, fontFamily: fonts.body, fontSize: 22, lineHeight: 24, textAlign: 'center', marginTop: 12},
  summary: {
    color: palette.accent3,
    fontFamily: fonts.body,
    fontSize: 22,
    textAlign: 'center',
    marginTop: 14,
    marginBottom: 20,
  },
  confirm: {marginTop: 12},
});
