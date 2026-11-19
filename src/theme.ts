import React from 'react';
import { ViewStyle, TextStyle } from 'react-native';

export const colors = {
  background: '#0b0b10',
  card: '#101217',
  accent: '#ff6b6b',
  accent2: '#6b9bff',
  text: '#ffffff'
};

export const styles: { container: ViewStyle; title: TextStyle } = {
  container: { flex: 1, backgroundColor: colors.background },
  title: { color: colors.text, fontSize: 24, fontWeight: '700' }
};

export const ThemeProvider: React.FC<{children?: React.ReactNode}> = ({children}) => {
  return <>{children}</>;
};
