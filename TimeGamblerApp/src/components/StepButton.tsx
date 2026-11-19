import React, {useEffect, useRef, useState} from 'react';
import {Pressable, StyleSheet, Text} from 'react-native';
import PixelFrame from './PixelFrame';
import {fonts, palette, PIXEL} from '../theme';

type Props = {
  label: string;
  /** Called once on touch-down, then repeatedly (speeding up) while held. */
  onStep: () => void;
  disabled?: boolean;
  color?: string;
  size?: number;
};

const HOLD_DELAY_MS = 350;
const FIRST_REPEAT_MS = 140;
const FASTEST_REPEAT_MS = 45;

/**
 * +/− button that steps on touch-down instead of on release, and keeps stepping while held,
 * so changing a value never waits for the finger to lift.
 */
export default function StepButton({label, onStep, disabled, color = palette.accent2, size = 56}: Props) {
  const [down, setDown] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // The latest callback, so a held button keeps stepping from the current value.
  const step = useRef(onStep);
  step.current = onStep;

  const stop = () => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    setDown(false);
  };

  const start = () => {
    setDown(true);
    step.current();
    let interval = FIRST_REPEAT_MS;
    const repeat = () => {
      step.current();
      interval = Math.max(FASTEST_REPEAT_MS, interval * 0.85);
      timer.current = setTimeout(repeat, interval);
    };
    timer.current = setTimeout(repeat, HOLD_DELAY_MS);
  };

  // Hitting the limit disables the button mid-hold; stop repeating then, and on unmount.
  useEffect(() => {
    if (disabled) {
      stop();
    }
  }, [disabled]);
  useEffect(() => stop, []);

  const pressed = down && !disabled;
  return (
    <Pressable
      onPressIn={start}
      onPressOut={stop}
      disabled={disabled}
      hitSlop={8}
      accessibilityRole="button"
      accessibilityLabel={label === '+' ? 'Increase' : 'Decrease'}>
      <PixelFrame
        color={disabled ? palette.panelAlt : color}
        bevel={!disabled}
        sunken={pressed}
        shadow={!pressed}
        style={[{width: size, height: size}, pressed && styles.pressed]}
        contentStyle={styles.content}>
        <Text style={[styles.label, {color: disabled ? palette.muted : '#fff'}]}>{label}</Text>
      </PixelFrame>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressed: {transform: [{translateX: PIXEL}, {translateY: PIXEL}], marginRight: PIXEL, marginBottom: PIXEL},
  content: {flex: 1, alignItems: 'center', justifyContent: 'center', padding: 0},
  label: {fontFamily: fonts.pixel, fontSize: 24, lineHeight: 28},
});
