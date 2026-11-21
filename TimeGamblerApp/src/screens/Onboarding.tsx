import React, {useCallback, useEffect, useState} from 'react';
import {
  Alert,
  AppState,
  PermissionsAndroid,
  Platform,
  Pressable,
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
    accessibilityDisclosureAccepted: false,
  });
  const [disclosureAccepted, setDisclosureAccepted] = useState(false);
  const ready = status.usageAccess && status.blocking && disclosureAccepted;

  const refresh = useCallback(() => {
    nativeBridge
      .getPermissionStatus()
      .then(next => {
        setStatus(next);
        setDisclosureAccepted(next.accessibilityDisclosureAccepted);
      })
      .catch(() => {});
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

  const acceptDisclosure = async () => {
    try {
      await nativeBridge.acceptAccessibilityDisclosure();
      setDisclosureAccepted(true);
    } catch (e) {
      Alert.alert('Could not save consent', String((e as Error)?.message ?? e));
    }
  };

  const openBlockerSettings = async () => {
    try {
      await nativeBridge.openBlockerSettings();
    } catch (e) {
      Alert.alert('Could not open Accessibility settings', String((e as Error)?.message ?? e));
    }
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
          why="Covers a protected app after its daily time runs out."
          how={
            'Tap "Installed apps" (or "Downloaded apps"), choose "Time Gambler" and switch it on. If Android says it\'s a restricted setting, open App info for Time Gambler, tap ⋮ and choose "Allow restricted settings", then try again.'
          }
          disclosureAccepted={disclosureAccepted}
          disclosure="Time Gambler uses Android Accessibility Service to detect when you open or switch apps. It reads only the foreground app identity (package and activity name) to apply your time limit and show the blocker when that limit runs out. It does not read screen text or other window content, record keystrokes, or take screenshots. This information is processed on this device and is not sent to a Time Gambler server or shared."
          onAcceptDisclosure={acceptDisclosure}
          actionLabel="Open Accessibility settings"
          onAction={openBlockerSettings}
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
  disclosure?: string;
  disclosureAccepted?: boolean;
  onAcceptDisclosure?: () => void;
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
      {props.disclosure && (
        <View style={styles.disclosure}>
          <Text style={styles.disclosureTitle}>Accessibility Service disclosure</Text>
          <Text style={styles.disclosureText}>{props.disclosure}</Text>
          <Pressable
            accessibilityRole="checkbox"
            accessibilityState={{checked: !!props.disclosureAccepted}}
            onPress={props.onAcceptDisclosure}
            style={styles.consentRow}>
            <View style={[styles.checkbox, props.disclosureAccepted && styles.checkboxChecked]}>
              {props.disclosureAccepted && <Text style={styles.checkmark}>✓</Text>}
            </View>
            <Text style={styles.consentText}>
              I understand and agree to this use of Accessibility Service.
            </Text>
          </Pressable>
        </View>
      )}
      {props.how && !props.granted && <Text style={styles.how}>{props.how}</Text>}
      {!props.granted && (!props.disclosure || props.disclosureAccepted) && (
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
  disclosure: {marginTop: 12, padding: 10, borderWidth: 1, borderColor: palette.accent3},
  disclosureTitle: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 8, lineHeight: 14},
  disclosureText: {color: palette.text, fontFamily: fonts.body, fontSize: 18, lineHeight: 19, marginTop: 6},
  consentRow: {flexDirection: 'row', alignItems: 'center', marginTop: 10},
  checkbox: {
    width: 24,
    height: 24,
    borderWidth: 2,
    borderColor: palette.accent3,
    marginRight: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxChecked: {backgroundColor: palette.accent3},
  checkmark: {color: palette.background, fontFamily: fonts.pixel, fontSize: 10},
  consentText: {color: palette.text, fontFamily: fonts.body, fontSize: 18, flex: 1},
  how: {color: palette.accent3, fontFamily: fonts.body, fontSize: 19, lineHeight: 20, marginTop: 8},
  cardButton: {marginTop: 14},
  footer: {padding: 20, paddingTop: 8},
});
