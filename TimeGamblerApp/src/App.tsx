import React, {useEffect, useState} from 'react';
import {ActivityIndicator, StatusBar, StyleSheet, View} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import Onboarding from './screens/Onboarding';
import Dashboard from './screens/Dashboard';
import AppSelection from './screens/AppSelection';
import TimerConfig from './screens/TimerConfig';
import TeenPatti from './screens/TeenPatti';
import {nativeBridge} from './nativeBridge';
import {RootStackParamList} from './types';
import {palette} from './theme';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  const [initialRoute, setInitialRoute] = useState<keyof RootStackParamList | null>(null);

  useEffect(() => {
    nativeBridge
      .getPermissionStatus()
      .then(status =>
        setInitialRoute(status.usageAccess && status.blocking ? 'Dashboard' : 'Onboarding'),
      )
      .catch(() => setInitialRoute('Onboarding'));
  }, []);

  if (!initialRoute) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={palette.accent3} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <StatusBar barStyle="light-content" backgroundColor={palette.background} />
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{headerShown: false, contentStyle: {backgroundColor: palette.background}}}>
        <Stack.Screen name="Onboarding" component={Onboarding} />
        <Stack.Screen name="Dashboard" component={Dashboard} />
        <Stack.Screen name="AppSelection" component={AppSelection} />
        <Stack.Screen name="TimerConfig" component={TimerConfig} />
        <Stack.Screen name="TeenPatti" component={TeenPatti} options={{gestureEnabled: false}} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    backgroundColor: palette.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
