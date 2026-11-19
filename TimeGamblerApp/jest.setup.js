import {NativeModules} from 'react-native';

// The Kotlin bridge does not exist under Jest; provide a stand-in with an empty state.
NativeModules.TimeGambler = {
  getInstalledApps: jest.fn(() => Promise.resolve([])),
  getSnapshot: jest.fn(() => Promise.resolve({apps: [], usageAccess: true, monitorRunning: true})),
  setAppLimit: jest.fn(),
  cancelAppLimit: jest.fn(() => Promise.resolve()),
  getPermissionStatus: jest.fn(() =>
    Promise.resolve({usageAccess: true, notifications: true, blocking: true}),
  ),
  openUsageAccessSettings: jest.fn(() => Promise.resolve()),
  openBlockerSettings: jest.fn(() => Promise.resolve()),
  startMonitoring: jest.fn(() => Promise.resolve()),
  getGambleOdds: jest.fn(() =>
    Promise.resolve({tier: 'FAIR', pressure: 0, playerWinChance: 0.49945}),
  ),
  playTeenPatti: jest.fn(),
  debugResetAll: jest.fn(() => Promise.resolve()),
};
