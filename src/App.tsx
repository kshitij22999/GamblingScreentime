import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import Dashboard from './screens/Dashboard';
import AppSelection from './screens/AppSelection';
import TimerConfig from './screens/TimerConfig';
import { ThemeProvider } from './theme';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <ThemeProvider>
      <NavigationContainer>
        <Stack.Navigator screenOptions={{headerShown: false}}>
          <Stack.Screen name="Dashboard" component={Dashboard} />
          <Stack.Screen name="AppSelection" component={AppSelection} />
          <Stack.Screen name="TimerConfig" component={TimerConfig} />
        </Stack.Navigator>
      </NavigationContainer>
    </ThemeProvider>
  );
}
