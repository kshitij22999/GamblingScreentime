export type InstalledApp = {
  packageName: string;
  appName: string;
};

/** Mirrors the Kotlin AppBudget. Kotlin is the source of truth; the UI only displays it. */
export type ProtectedApp = InstalledApp & {
  dailyLimitMinutes: number;
  remainingSeconds: number;
  lastLimitChangeMs: number;
};

export type Snapshot = {
  apps: ProtectedApp[];
  usageAccess: boolean;
  monitorRunning: boolean;
};

export type PermissionStatus = {
  usageAccess: boolean;
  notifications: boolean;
  /** The blocker accessibility service is switched on. */
  blocking: boolean;
  accessibilityDisclosureAccepted: boolean;
};

/** How hard the house plays, based on last week's use of the app. Decided in Kotlin. */
export type GambleOdds = {
  tier: 'FAIR' | 'LEANING' | 'TOUGH' | 'HARSH';
  /** 0 = no overuse, 1 = heavy overuse. */
  pressure: number;
  playerWinChance: number;
};

export type PlayingCard = {
  rank: string;
  suit: 'SPADES' | 'HEARTS' | 'DIAMONDS' | 'CLUBS';
};

export type TeenPattiHand = {
  cards: PlayingCard[];
  hand: 'TRAIL' | 'PURE_SEQUENCE' | 'SEQUENCE' | 'COLOR' | 'PAIR' | 'HIGH_CARD';
};

/** A settled round. The balance change is already saved when this arrives. */
export type TeenPattiResult = {
  gameId: string;
  packageName: string;
  outcome: 'PLAYER_WIN' | 'HOUSE_WIN' | 'TIE';
  stakeSeconds: number;
  balanceBeforeSeconds: number;
  balanceAfterSeconds: number;
  player: TeenPattiHand;
  house: TeenPattiHand;
  odds: GambleOdds;
  /** True when this gameId was already played and the stored result was returned. */
  replayed: boolean;
};

export type RootStackParamList = {
  Onboarding: undefined;
  Dashboard: undefined;
  AppSelection: undefined;
  TimerConfig: {app: InstalledApp; existing?: ProtectedApp};
  TeenPatti: {app: ProtectedApp};
};
