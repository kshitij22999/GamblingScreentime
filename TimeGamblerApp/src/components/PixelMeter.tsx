import React, {useMemo, useRef} from 'react';
import {PanResponder, StyleSheet, View} from 'react-native';
import PixelFrame from './PixelFrame';
import {palette, PIXEL} from '../theme';

type Props = {
  /** Number of filled segments, 1..segments. */
  value: number;
  segments: number;
  onChange: (value: number) => void;
  color?: string;
};

/**
 * Segmented 8-bit power bar. Tap or drag across it to set the value; it follows the finger
 * directly, with no animation in between.
 */
export default function PixelMeter({value, segments, onChange, color = palette.accent3}: Props) {
  const width = useRef(1);
  const last = useRef(value);
  last.current = value;
  const change = useRef(onChange);
  change.current = onChange;

  const responder = useMemo(() => {
    const update = (x: number) => {
      const next = Math.min(segments, Math.max(1, Math.ceil((x / width.current) * segments)));
      if (next !== last.current) {
        last.current = next;
        change.current(next);
      }
    };
    return PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      // Keep the gesture when the finger drifts vertically, so a ScrollView can't steal it.
      onPanResponderTerminationRequest: () => false,
      onPanResponderGrant: e => update(e.nativeEvent.locationX),
      onPanResponderMove: e => update(e.nativeEvent.locationX),
    });
  }, [segments]);

  return (
    <PixelFrame color={palette.panelAlt} sunken bevel contentStyle={styles.frame}>
      <View
        style={styles.track}
        onLayout={e => (width.current = Math.max(1, e.nativeEvent.layout.width))}
        {...responder.panHandlers}>
        {Array.from({length: segments}, (_, i) => (
          <View
            key={i}
            pointerEvents="none"
            style={[styles.segment, {backgroundColor: i < value ? color : '#202838'}]}
          />
        ))}
      </View>
    </PixelFrame>
  );
}

const styles = StyleSheet.create({
  frame: {padding: PIXEL * 2},
  track: {flexDirection: 'row', height: PIXEL * 8},
  segment: {flex: 1, marginHorizontal: 1},
});
