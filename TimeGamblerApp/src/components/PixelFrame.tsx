import React from 'react';
import {StyleProp, StyleSheet, View, ViewStyle} from 'react-native';
import {palette, PIXEL} from '../theme';

type Props = {
  color?: string;
  borderColor?: string;
  /** Hard black drop shadow, offset one pixel down and right. */
  shadow?: boolean;
  /** Light top-left and dark bottom-right edges, like a raised NES button. */
  bevel?: boolean;
  /** Draws the bevel inverted, for a pushed-in look. */
  sunken?: boolean;
  style?: StyleProp<ViewStyle>;
  /** Padding inside the outline. */
  contentStyle?: StyleProp<ViewStyle>;
  children?: React.ReactNode;
};

/**
 * 8-bit box: a one-pixel outline with notched (stepped) corners instead of rounded ones. Built
 * from plain rectangles so it costs no images and scales with layout.
 */
export default function PixelFrame({
  color = palette.panel,
  borderColor = palette.ink,
  shadow,
  bevel,
  sunken,
  style,
  contentStyle,
  children,
}: Props) {
  const light = sunken ? SHADE : HIGHLIGHT;
  const dark = sunken ? HIGHLIGHT : SHADE;
  return (
    <View style={[shadow && styles.shadowSpace, style]}>
      {shadow && <Notched inset={0} color={palette.ink} offset={PIXEL} />}
      <Notched inset={0} color={borderColor} />
      <Notched inset={PIXEL} color={color} />
      {bevel && (
        <>
          <View style={[styles.edge, {top: PIXEL, left: PIXEL * 2, right: PIXEL * 2, height: PIXEL, backgroundColor: light}]} />
          <View style={[styles.edge, {left: PIXEL, top: PIXEL * 2, bottom: PIXEL * 2, width: PIXEL, backgroundColor: light}]} />
          <View style={[styles.edge, {bottom: PIXEL, left: PIXEL * 2, right: PIXEL * 2, height: PIXEL, backgroundColor: dark}]} />
          <View style={[styles.edge, {right: PIXEL, top: PIXEL * 2, bottom: PIXEL * 2, width: PIXEL, backgroundColor: dark}]} />
        </>
      )}
      <View style={[styles.content, contentStyle]}>{children}</View>
    </View>
  );
}

/** A rectangle with one-pixel corner notches: a wide bar and a tall bar overlapping. */
function Notched({inset, color, offset = 0}: {inset: number; color: string; offset?: number}) {
  const edge = inset + offset;
  return (
    <>
      <View
        style={[
          styles.edge,
          {top: inset + offset, bottom: inset - offset, left: edge + PIXEL, right: inset - offset + PIXEL, backgroundColor: color},
        ]}
      />
      <View
        style={[
          styles.edge,
          {top: edge + PIXEL, bottom: inset - offset + PIXEL, left: inset + offset, right: inset - offset, backgroundColor: color},
        ]}
      />
    </>
  );
}

const HIGHLIGHT = 'rgba(255,255,255,0.35)';
const SHADE = 'rgba(0,0,0,0.35)';

const styles = StyleSheet.create({
  shadowSpace: {marginRight: PIXEL, marginBottom: PIXEL},
  edge: {position: 'absolute'},
  content: {padding: PIXEL * 3},
});
