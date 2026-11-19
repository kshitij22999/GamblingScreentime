import React from 'react';
import {Animated, StyleSheet, Text, View} from 'react-native';
import {PlayingCard as Card} from '../types';
import {isRedSuit, SUIT_SYMBOLS} from '../teenPatti';
import {fonts, palette, PIXEL} from '../theme';

type Props = {
  card?: Card;
  /** 0 = dealt face down, 1 = face up. Drives a squash-and-swap flip. */
  flip: Animated.Value;
  /** 0 = still in the shoe, 1 = on the table. */
  dealt: Animated.Value;
  highlight?: boolean;
};

export const CARD_WIDTH = 82;
export const CARD_HEIGHT = 118;

export default function PlayingCard({card, flip, dealt, highlight}: Props) {
  // Squash to nothing at the midpoint, then show the other side.
  const scaleX = flip.interpolate({inputRange: [0, 0.5, 1], outputRange: [1, 0, 1]});
  const backOpacity = flip.interpolate({inputRange: [0, 0.5, 0.501, 1], outputRange: [1, 1, 0, 0]});
  const faceOpacity = flip.interpolate({inputRange: [0, 0.5, 0.501, 1], outputRange: [0, 0, 1, 1]});
  const translateY = dealt.interpolate({inputRange: [0, 1], outputRange: [-60, 0]});

  const color = card && isRedSuit(card.suit) ? '#d6334a' : '#1a1c26';
  const symbol = card ? SUIT_SYMBOLS[card.suit] : '';

  return (
    <Animated.View
      style={[styles.slot, {opacity: dealt, transform: [{translateY}, {scaleX}]}]}>
      <Animated.View style={[styles.card, styles.back, {opacity: backOpacity}]}>
        <View style={styles.backInner}>
          <Text style={styles.backMark}>◆</Text>
        </View>
      </Animated.View>
      <Animated.View
        style={[
          styles.card,
          styles.face,
          highlight && styles.highlight,
          {opacity: faceOpacity},
        ]}>
        {card && (
          <>
            <Text style={[styles.corner, {color}]}>
              {card.rank}
              {'\n'}
              {symbol}
            </Text>
            <Text style={[styles.pip, {color}]}>{symbol}</Text>
          </>
        )}
      </Animated.View>
    </Animated.View>
  );
}

// Square 8-bit cards: thick black outline and a hard shadow edge on the bottom and right.
const styles = StyleSheet.create({
  slot: {width: CARD_WIDTH, height: CARD_HEIGHT, marginHorizontal: 5},
  card: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderWidth: PIXEL,
    borderColor: palette.ink,
    borderRightWidth: PIXEL * 2,
    borderBottomWidth: PIXEL * 2,
  },
  back: {backgroundColor: palette.accent, padding: PIXEL},
  backInner: {
    flex: 1,
    borderWidth: PIXEL,
    borderColor: '#ffffff55',
    alignItems: 'center',
    justifyContent: 'center',
  },
  backMark: {color: '#ffffffaa', fontSize: 28},
  face: {backgroundColor: '#f7f2e8', padding: 6},
  highlight: {borderColor: palette.accent3},
  corner: {fontFamily: fonts.pixel, fontSize: 14, lineHeight: 20},
  pip: {position: 'absolute', bottom: 6, right: 8, fontSize: 36},
});
