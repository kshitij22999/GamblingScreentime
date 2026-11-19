import React, {useCallback, useEffect, useState} from 'react';
import {
  AppState,
  PermissionsAndroid,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import PixelButton from '../components/PixelButton';
import PixelFrame from '../components/PixelFrame';
import {nativeBridge} from '../nativeBridge';
import {PermissionStatus, RootStackParamList} from '../types';
import {fonts, palette} from '../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'Onboarding'>;

export default function Onboarding({navigation}: Props) {
  const [status, setStatus] = useState<PermissionStatus>({
    usageAccess: false,
    notifications: false,
    blocking: false,
  });
  const ready = status.usageAccess && status.blocking;

  const refresh = useCallback(() => {
    nativeBridge.getPermissionStatus().then(setStatus).catch(() => {});
  }, []);

  // Usage Access is granted in system Settings, so re-check whenever the user comes back.
  useEffect(() => {
    refresh();
    const sub = AppState.addEventListener('change', state => state === 'active' && refresh());
    return () => sub.remove();
  }, [refresh]);

  const requestNotifications = async () => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS);
    }
    refresh();
  };

  const finish = async () => {
    await nativeBridge.startMonitoring();
    navigation.reset({index: 0, routes: [{name: 'Dashboard'}]});
  };

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Before we deal you in</Text>
        <Text style={styles.subtitle}>
          Time Gambler runs entirely on this phone. No account, no servers, nothing leaves the device.
        </Text>

        <PermissionCard
          step="1"
          title="Usage Access"
          required
          granted={status.usageAccess}
          why="Lets Time Gambler see which app is on screen, so a timer only runs while you are actually using that app. Without it, nothing can be tracked."
          how={'Find "Time Gambler" in the list and switch on "Permit usage access".'}
          actionLabel="Open Usage Access settings"
          onAction={() => nativeBridge.openUsageAccessSettings()}
        />

        <PermissionCard
          step="2"
          title="App blocker"
          required
          granted={status.blocking}
          why="Covers an app the moment its time runs out, before you can use it. It only sees which app is open, never what's inside it."
          how={
            'Tap "Installed apps" (or "Downloaded apps"), choose "Time Gambler" and switch it on. If Android says it\'s a restricted setting, open App info for Time Gambler, tap ⋮ and choose "Allow restricted settings", then try again.'
          }
          actionLabel="Open Accessibility settings"
          onAction={() => nativeBridge.openBlockerSettings()}
        />

        <PermissionCard
          step="3"
          title="Notifications"
          granted={status.notifications}
          why="Shows a live countdown while you use a protected app, and tells you monitoring is on."
          actionLabel="Allow notifications"
          onAction={requestNotifications}
        />
      </ScrollView>

      <View style={styles.footer}>
        <PixelButton
          label={ready ? "Let's play" : 'Grant the required permissions'}
          disabled={!ready}
          onPress={finish}
        />
      </View>
    </SafeAreaView>
  );
}

function PermissionCard(props: {
  step: string;
  title: string;
  required?: boolean;
  granted: boolean;
  why: string;
  how?: string;
  actionLabel: string;
  onAction: () => void;
}) {
  return (
    <PixelFrame
      shadow
      borderColor={props.granted ? palette.success : palette.ink}
      style={styles.card}
      contentStyle={styles.cardContent}>
      <View style={styles.cardHeader}>
        <PixelFrame color={palette.accent3} bevel style={styles.stepBadge} contentStyle={styles.stepContent}>
          <Text style={styles.stepText}>{props.step}</Text>
        </PixelFrame>
        <Text style={styles.cardTitle}>{props.title}</Text>
        <Text style={[styles.tag, {color: props.granted ? palette.success : palette.muted}]}>
          {props.granted ? 'GRANTED' : props.required ? 'REQUIRED' : 'RECOMMENDED'}
        </Text>
      </View>
      <Text style={styles.why}>{props.why}</Text>
      {props.how && !props.granted && <Text style={styles.how}>{props.how}</Text>}
      {!props.granted && (
        <PixelButton
          small
          label={props.actionLabel}
          onPress={props.onAction}
          color={palette.accent2}
          textColor="#fff"
          style={styles.cardButton}
        />
      )}
    </PixelFrame>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: palette.background},
  content: {padding: 20, paddingBottom: 32},
  title: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 18, lineHeight: 28, marginTop: 12},
  subtitle: {color: palette.muted, fontFamily: fonts.body, fontSize: 21, lineHeight: 22, marginTop: 10, marginBottom: 20},
  card: {marginBottom: 16},
  cardContent: {padding: 16},
  cardHeader: {flexDirection: 'row', alignItems: 'center', marginBottom: 10},
  stepBadge: {width: 32, height: 32, marginRight: 10},
  stepContent: {flex: 1, padding: 0, alignItems: 'center', justifyContent: 'center'},
  stepText: {color: '#1a1d22', fontFamily: fonts.pixel, fontSize: 12, lineHeight: 16},
  cardTitle: {color: palette.text, fontFamily: fonts.pixel, fontSize: 11, lineHeight: 16, flex: 1},
  tag: {fontFamily: fonts.pixel, fontSize: 7, marginLeft: 6},
  why: {color: palette.text, fontFamily: fonts.body, fontSize: 20, lineHeight: 21, opacity: 0.9},
  how: {color: palette.accent3, fontFamily: fonts.body, fontSize: 19, lineHeight: 20, marginTop: 8},
  cardButton: {marginTop: 14},
  footer: {padding: 20, paddingTop: 8},
});
