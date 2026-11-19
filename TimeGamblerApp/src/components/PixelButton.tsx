import React, {useState} from 'react';
import {Pressable, StyleProp, StyleSheet, Text, ViewStyle} from 'react-native';
import PixelFrame from './PixelFrame';
import {fonts, palette, PIXEL} from '../theme';

type Props = {
  label: string;
  onPress: () => void;
  color?: string;
  textColor?: string;
  disabled?: boolean;
  /** Smaller text and padding, for inline buttons. */
  small?: boolean;
  style?: StyleProp<ViewStyle>;
};

/** 8-bit button. On touch-down it drops into its shadow, so it responds before the finger lifts. */
export default function PixelButton({
  label,
  onPress,
  color = palette.accent,
  textColor = '#120d0d',
  disabled,
  small,
  style,
}: Props) {
  const [down, setDown] = useState(false);
  const pressed = down && !disabled;
  return (
    <Pressable
      onPress={onPress}
      onPressIn={() => setDown(true)}
      onPressOut={() => setDown(false)}
      disabled={disabled}
      accessibilityRole="button"
      accessibilityState={{disabled: !!disabled}}
      style={style}>
      <PixelFrame
        color={disabled ? palette.panelAlt : color}
        bevel={!disabled}
        sunken={pressed}
        shadow={!pressed}
        style={pressed && styles.pressed}
        contentStyle={small ? styles.contentSmall : styles.content}>
        <Text
          style={[styles.label, small && styles.labelSmall, {color: disabled ? palette.muted : textColor}]}
          numberOfLines={1}
          adjustsFontSizeToFit>
          {label.toUpperCase()}
        </Text>
      </PixelFrame>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // Shadow is gone while pressed, so take its space back as an offset: the face sinks in place.
  pressed: {transform: [{translateX: PIXEL}, {translateY: PIXEL}], marginRight: PIXEL, marginBottom: PIXEL},
  content: {paddingVertical: PIXEL * 4, paddingHorizontal: PIXEL * 4, alignItems: 'center'},
  contentSmall: {paddingVertical: PIXEL * 2.5, paddingHorizontal: PIXEL * 3, alignItems: 'center'},
  label: {fontFamily: fonts.pixel, fontSize: 14, lineHeight: 20},
  labelSmall: {fontSize: 10, lineHeight: 14},
});
