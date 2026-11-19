import React, {useCallback, useState} from 'react';
import {
  Alert,
  FlatList,
  Linking,
  PermissionsAndroid,
  Platform,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import AppCard from '../components/AppCard';
import PixelButton from '../components/PixelButton';
import PixelFrame from '../components/PixelFrame';
import {nativeBridge} from '../nativeBridge';
import {RootStackParamList, Snapshot} from '../types';
import {fonts, palette} from '../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'Dashboard'>;

const POLL_INTERVAL_MS = 1000;

export default function Dashboard({navigation}: Props) {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [notificationsOn, setNotificationsOn] = useState(true);
  const [blockingOn, setBlockingOn] = useState(true);

  const checkNotifications = useCallback(() => {
    nativeBridge
      .getPermissionStatus()
      .then(status => {
        setNotificationsOn(status.notifications);
        setBlockingOn(status.blocking);
      })
      .catch(() => {});
  }, []);

  // The live countdown lives in the monitoring notification, so it needs notification access.
  const enableNotifications = async () => {
    const result =
      Platform.OS === 'android' && Platform.Version >= 33
        ? await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS)
        : PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN;
    if (result !== PermissionsAndroid.RESULTS.GRANTED) {
      await Linking.openSettings();
    }
    checkNotifications();
  };

  // Kotlin owns the timers; the dashboard just polls and renders while it is on screen.
  useFocusEffect(
    useCallback(() => {
      let active = true;
      const load = () => {
        nativeBridge
          .getSnapshot()
          .then(next => active && setSnapshot(next))
          .catch(() => {});
        checkNotifications();
      };
      nativeBridge.startMonitoring().catch(() => {});
      load();
      const interval = setInterval(load, POLL_INTERVAL_MS);
      return () => {
        active = false;
        clearInterval(interval);
      };
    }, [checkNotifications]),
  );

  const apps = snapshot?.apps ?? [];

  // TESTING ONLY: remove this and the button below before release.
  const confirmReset = () =>
    Alert.alert('Reset everything?', 'Removes all protected apps and game history.', [
      {text: 'Cancel', style: 'cancel'},
      {
        text: 'Reset',
        style: 'destructive',
        // Start from a resolved promise so a synchronous throw (e.g. a stale native build
        // without this method) lands in .catch instead of the red error screen.
        onPress: () =>
          Promise.resolve()
            .then(() => nativeBridge.debugResetAll())
            .then(() => nativeBridge.getSnapshot())
            .then(setSnapshot)
            .catch(e => Alert.alert('Reset failed', String(e?.message ?? e))),
      },
    ]);

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.header}>
        {__DEV__ && (
          <Pressable style={styles.resetButton} onPress={confirmReset}>
            <Text style={styles.resetText}>RESET</Text>
          </Pressable>
        )}
        <Text style={styles.title}>TIME GAMBLER</Text>
        <Text style={styles.subtitle}>Every app gets its own clock. Spend it wisely.</Text>
        {apps.length > 0 && (
          <Text style={styles.hint}>
            Clocks are paused while you're here. Open a protected app and its live countdown shows in your notifications.
          </Text>
        )}
      </View>

      {snapshot && !snapshot.usageAccess && (
        <Banner tone="warning" onPress={() => navigation.navigate('Onboarding')}>
          Usage Access is off, so timers can't run. Tap to fix.
        </Banner>
      )}

      {snapshot && snapshot.usageAccess && !blockingOn && (
        <Banner tone="warning" onPress={() => nativeBridge.openBlockerSettings()}>
          Blocking is off, so apps stay usable after their time runs out. Tap to switch on the Time
          Gambler app blocker in Accessibility settings.
        </Banner>
      )}

      {snapshot && snapshot.usageAccess && apps.length > 0 && !notificationsOn && (
        <Banner tone="notice" onPress={enableNotifications}>
          Turn on notifications to see a live countdown while you use a protected app.
        </Banner>
      )}

      {snapshot && apps.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyTitle}>No protected apps yet</Text>
          <Text style={styles.emptyText}>Pick an app and give it a daily time budget.</Text>
        </View>
      ) : (
        <FlatList
          contentContainerStyle={styles.list}
          data={apps}
          keyExtractor={item => item.packageName}
          renderItem={({item}) => (
            <AppCard
              app={item}
              onPress={() => navigation.navigate('TimerConfig', {app: item, existing: item})}
              onGamble={() => navigation.navigate('TeenPatti', {app: item})}
            />
          )}
        />
      )}

      <View style={styles.footer}>
        <PixelButton
          label="+ Protect an app"
          color={palette.accent2}
          textColor="#fff"
          onPress={() => navigation.navigate('AppSelection')}
        />
      </View>
    </SafeAreaView>
  );
}

/** Tappable alert strip: red for problems that stop the app working, yellow for suggestions. */
function Banner({tone, onPress, children}: {tone: 'warning' | 'notice'; onPress: () => void; children: React.ReactNode}) {
  const warning = tone === 'warning';
  return (
    <Pressable onPress={onPress} style={styles.banner}>
      <PixelFrame
        color={warning ? '#3a1a1f' : '#2e2614'}
        borderColor={warning ? palette.danger : palette.accent3}
        contentStyle={styles.bannerContent}>
        <Text style={styles.bannerText}>
          {warning ? '! ' : '> '}
          {children}
        </Text>
      </PixelFrame>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: palette.background},
  header: {paddingHorizontal: 18, paddingTop: 18, paddingBottom: 8},
  resetButton: {
    alignSelf: 'flex-start',
    marginBottom: 10,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderWidth: 2,
    borderColor: palette.danger,
  },
  resetText: {color: palette.danger, fontFamily: fonts.pixel, fontSize: 8},
  title: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 22, lineHeight: 30},
  subtitle: {color: palette.muted, fontFamily: fonts.body, fontSize: 21, marginTop: 6},
  hint: {color: palette.accent2, fontFamily: fonts.body, fontSize: 19, lineHeight: 20, marginTop: 8},
  banner: {marginHorizontal: 18, marginTop: 8},
  bannerContent: {padding: 12},
  bannerText: {color: palette.text, fontFamily: fonts.body, fontSize: 20, lineHeight: 21},
  list: {padding: 18, paddingBottom: 24},
  emptyState: {flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24},
  emptyTitle: {color: palette.text, fontFamily: fonts.pixel, fontSize: 12, lineHeight: 20, textAlign: 'center'},
  emptyText: {color: palette.muted, fontFamily: fonts.body, fontSize: 21, marginTop: 10, textAlign: 'center'},
  footer: {padding: 18, paddingTop: 4},
});
