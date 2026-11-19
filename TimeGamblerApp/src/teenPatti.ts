import {GambleOdds, PlayingCard, TeenPattiHand, TeenPattiResult} from './types';

/** UI mirror of Kotlin StakeRules. Kotlin re-checks every bet; this only shapes the picker. */
export const STAKE_STEP_MINUTES = 5;
export const MIN_STAKE_MINUTES = 5;
export const MAX_STAKE_MINUTES = 6 * 60;

/** Largest non-all-in bet for a balance, or 0 when the balance is too low to gamble. */
export function maxStakeMinutes(remainingSeconds: number): number {
  const whole = Math.floor(remainingSeconds / 60 / STAKE_STEP_MINUTES) * STAKE_STEP_MINUTES;
  return Math.min(MAX_STAKE_MINUTES, whole);
}

export function canGamble(remainingSeconds: number): boolean {
  return remainingSeconds >= MIN_STAKE_MINUTES * 60;
}

export function clampStake(minutes: number, remainingSeconds: number): number {
  const max = maxStakeMinutes(remainingSeconds);
  if (max < MIN_STAKE_MINUTES) {
    return 0;
  }
  const stepped = Math.round(minutes / STAKE_STEP_MINUTES) * STAKE_STEP_MINUTES;
  return Math.min(max, Math.max(MIN_STAKE_MINUTES, stepped));
}

/**
 * Idempotency key for one game attempt. It only needs to be unique on this device; the deal
 * itself uses SecureRandom in Kotlin.
 */
let counter = 0;
export function newGameId(): string {
  counter += 1;
  return `${Date.now().toString(36)}-${counter}-${Math.random().toString(36).slice(2, 10)}`;
}

export const HAND_LABELS: Record<TeenPattiHand['hand'], string> = {
  TRAIL: 'Trail',
  PURE_SEQUENCE: 'Pure Sequence',
  SEQUENCE: 'Sequence',
  COLOR: 'Color',
  PAIR: 'Pair',
  HIGH_CARD: 'High Card',
};

export const SUIT_SYMBOLS: Record<PlayingCard['suit'], string> = {
  SPADES: '♠',
  HEARTS: '♥',
  DIAMONDS: '♦',
  CLUBS: '♣',
};

export const isRedSuit = (suit: PlayingCard['suit']) => suit === 'HEARTS' || suit === 'DIAMONDS';

export const TIER_LABELS: Record<GambleOdds['tier'], {title: string; detail: string}> = {
  FAIR: {title: 'Fair table', detail: 'Your use of this app last week looks healthy.'},
  LEANING: {title: 'House leans', detail: 'You used this app a fair bit last week.'},
  TOUGH: {title: 'Tough table', detail: 'You used this app a lot last week.'},
  HARSH: {title: 'Harsh table', detail: 'This app ate a big chunk of your week.'},
};

/** Net change to the balance, in seconds. */
export const netSeconds = (r: TeenPattiResult) => r.balanceAfterSeconds - r.balanceBeforeSeconds;
