import {NativeModules} from 'react-native';
import {
  GambleOdds,
  InstalledApp,
  PermissionStatus,
  ProtectedApp,
  Snapshot,
  TeenPattiResult,
} from './types';

type TimeGamblerNative = {
  getInstalledApps(): Promise<InstalledApp[]>;
  getSnapshot(): Promise<Snapshot>;
  setAppLimit(packageName: string, appName: string, minutes: number): Promise<ProtectedApp>;
  cancelAppLimit(packageName: string): Promise<void>;
  getPermissionStatus(): Promise<PermissionStatus>;
  openUsageAccessSettings(): Promise<void>;
  acceptAccessibilityDisclosure(): Promise<void>;
  openBlockerSettings(): Promise<void>;
  startMonitoring(): Promise<void>;
  getGambleOdds(packageName: string): Promise<GambleOdds>;
  debugResetAll(): Promise<void>;
  playTeenPatti(
    gameId: string,
    packageName: string,
    stakeMinutes: number,
    allIn: boolean,
  ): Promise<TeenPattiResult>;
};

const native: TimeGamblerNative | undefined = NativeModules.TimeGambler;

function module(): TimeGamblerNative {
  if (!native) {
    // No silent fallback: enforcement lives in Kotlin, so a missing module is a real failure.
    throw new Error('TimeGambler native module is not linked');
  }
  return native;
}

export type BridgeErrorCode =
  | 'E_INVALID_LIMIT'
  | 'E_UNCHANGED'
  | 'E_NOT_PROTECTABLE'
  | 'E_NOT_PROTECTED'
  | 'E_STAKE_BALANCE_TOO_LOW'
  | 'E_STAKE_BELOW_MINIMUM'
  | 'E_STAKE_ABOVE_MAXIMUM'
  | 'E_STAKE_NOT_A_STEP'
  | 'E_STAKE_MORE_THAN_BALANCE'
  | string;

export type BridgeError = Error & {code?: BridgeErrorCode};

export const nativeBridge = {
  getInstalledApps: () => module().getInstalledApps(),
  getSnapshot: () => module().getSnapshot(),
  setAppLimit: (app: InstalledApp, minutes: number) =>
    module().setAppLimit(app.packageName, app.appName, minutes),
  /** Removes the app's limit entirely. */
  cancelAppLimit: (packageName: string) => module().cancelAppLimit(packageName),
  getPermissionStatus: () => module().getPermissionStatus(),
  openUsageAccessSettings: () => module().openUsageAccessSettings(),
  acceptAccessibilityDisclosure: () => module().acceptAccessibilityDisclosure(),
  openBlockerSettings: () => module().openBlockerSettings(),
  startMonitoring: () => module().startMonitoring(),
  getGambleOdds: (packageName: string) => module().getGambleOdds(packageName),
  /** TESTING ONLY: wipes all timers and game history. Debug builds only. */
  debugResetAll: () => module().debugResetAll(),
  /** Reuse the same gameId when retrying a call so a round is never paid out twice. */
  playTeenPatti: (gameId: string, packageName: string, stakeMinutes: number, allIn = false) =>
    module().playTeenPatti(gameId, packageName, stakeMinutes, allIn),
};
