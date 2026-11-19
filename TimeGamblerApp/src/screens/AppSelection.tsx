import React, {useEffect, useMemo, useState} from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {nativeBridge} from '../nativeBridge';
import {InstalledApp, RootStackParamList} from '../types';
import PixelFrame from '../components/PixelFrame';
import {fonts, palette} from '../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'AppSelection'>;

export default function AppSelection({navigation}: Props) {
  const [apps, setApps] = useState<InstalledApp[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  useEffect(() => {
    Promise.all([nativeBridge.getInstalledApps(), nativeBridge.getSnapshot()])
      .then(([installed, snapshot]) => {
        const protectedPackages = new Set(snapshot.apps.map(a => a.packageName));
        setApps(installed.filter(a => !protectedPackages.has(a.packageName)));
      })
      .catch(e => setError(String(e?.message ?? e)));
  }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (apps ?? []).filter(
      a => !q || a.appName.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q),
    );
  }, [apps, query]);

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.header}>
        <Text style={styles.title}>Pick an app</Text>
        <Text style={styles.subtitle}>It gets its own daily clock</Text>
        <PixelFrame color={palette.panelAlt} sunken bevel style={styles.searchFrame} contentStyle={styles.searchContent}>
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder="> search apps_"
            placeholderTextColor={palette.muted}
            style={styles.search}
          />
        </PixelFrame>
      </View>

      {error ? (
        <Text style={styles.message}>Couldn't load apps: {error}</Text>
      ) : !apps ? (
        <ActivityIndicator style={styles.loading} color={palette.accent3} />
      ) : (
        <FlatList
          contentContainerStyle={styles.list}
          data={filtered}
          keyExtractor={item => item.packageName}
          keyboardShouldPersistTaps="handled"
          ListEmptyComponent={<Text style={styles.message}>No matching apps</Text>}
          renderItem={({item}) => (
            <Pressable onPress={() => navigation.navigate('TimerConfig', {app: item})} style={styles.appRow}>
              {({pressed}) => (
                <PixelFrame color={pressed ? palette.panelAlt : palette.panel} contentStyle={styles.appRowContent}>
                  <PixelFrame color={palette.accent2} bevel style={styles.iconBadge} contentStyle={styles.iconContent}>
                    <Text style={styles.iconText}>{item.appName.charAt(0).toUpperCase()}</Text>
                  </PixelFrame>
                  <View style={styles.appText}>
                    <Text style={styles.appName} numberOfLines={1}>
                      {item.appName}
                    </Text>
                    <Text style={styles.appPackage} numberOfLines={1}>
                      {item.packageName}
                    </Text>
                  </View>
                  <Text style={styles.chevron}>{'>'}</Text>
                </PixelFrame>
              )}
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: palette.background},
  header: {paddingHorizontal: 18, paddingTop: 18, paddingBottom: 4},
  title: {color: palette.text, fontFamily: fonts.pixel, fontSize: 20, lineHeight: 28},
  subtitle: {color: palette.muted, fontFamily: fonts.body, fontSize: 21, marginTop: 4},
  searchFrame: {marginTop: 14},
  searchContent: {paddingVertical: 2, paddingHorizontal: 12},
  search: {color: palette.text, fontFamily: fonts.body, fontSize: 22, paddingVertical: 8},
  loading: {marginTop: 40},
  message: {color: palette.muted, fontFamily: fonts.body, fontSize: 20, textAlign: 'center', marginTop: 32, paddingHorizontal: 18},
  list: {padding: 18},
  appRow: {marginBottom: 10},
  appRowContent: {padding: 12, flexDirection: 'row', alignItems: 'center'},
  iconBadge: {width: 44, height: 44, marginRight: 12},
  iconContent: {flex: 1, padding: 0, alignItems: 'center', justifyContent: 'center'},
  iconText: {color: '#fff', fontFamily: fonts.pixel, fontSize: 16, lineHeight: 20},
  appText: {flex: 1},
  appName: {color: palette.text, fontFamily: fonts.pixel, fontSize: 11, lineHeight: 16},
  appPackage: {color: palette.muted, fontFamily: fonts.body, fontSize: 17, marginTop: 4},
  chevron: {color: palette.accent3, fontFamily: fonts.pixel, fontSize: 12, marginLeft: 8},
});
