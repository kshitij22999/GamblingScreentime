import React, {useEffect, useState} from 'react';
import {View, Text, FlatList, TouchableOpacity, SafeAreaView} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import AsyncStorage from '@react-native-async-storage/async-storage';

// In Phase1 this screen shows a stub list. Later replaced with native-installed-apps.
const STUB_APPS = [
  {packageName:'com.instagram.android', appName:'Instagram'},
  {packageName:'com.google.android.youtube', appName:'YouTube'},
  {packageName:'com.reddit.frontpage', appName:'Reddit'}
];

export default function AppSelection(){
  const [selected, setSelected] = useState<string | null>(null);
  const nav = useNavigation();

  const selectApp = (app:any) => {
    setSelected(app.packageName);
    nav.navigate('TimerConfig' as never, {app} as never);
  };

  return (
    <SafeAreaView style={{flex:1, backgroundColor:'#0b0b10'}}>
      <View style={{padding:16}}>
        <Text style={{color:'#fff', fontSize:22, fontWeight:'800'}}>Select an app</Text>
        <Text style={{color:'#9aa0b0', marginTop:4}}>Choose apps to protect</Text>
      </View>
      <FlatList
        data={STUB_APPS}
        keyExtractor={(i:any)=>i.packageName}
        contentContainerStyle={{padding:16}}
        renderItem={({item})=> (
          <TouchableOpacity onPress={()=>selectApp(item)} style={{backgroundColor:'#121218', padding:12, borderRadius:10, marginBottom:10}}>
            <Text style={{color:'#fff', fontWeight:'700'}}>{item.appName}</Text>
            <Text style={{color:'#9aa0b0'}}>{item.packageName}</Text>
          </TouchableOpacity>
        )}
      />
    </SafeAreaView>
  );
}
