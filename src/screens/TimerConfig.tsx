import React, {useState} from 'react';
import {View, Text, TouchableOpacity, SafeAreaView} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function TimerConfig({route, navigation}:any){
  const app = route?.params?.app;
  const [minutes, setMinutes] = useState<number>(60);

  const presets = [15,30,45,60,90,120];

  const save = async ()=>{
    const stored = await AsyncStorage.getItem('protectedApps');
    const list = stored ? JSON.parse(stored) : [];
    const now = Date.now();
    const entry = {
      packageName: app.packageName,
      appName: app.appName,
      dailyLimitSeconds: minutes*60,
      remainingSeconds: minutes*60,
      lastLimitChangeTimestamp: now,
      isBlocked: false
    };
    const filtered = list.filter((i:any)=>i.packageName !== app.packageName).concat(entry);
    await AsyncStorage.setItem('protectedApps', JSON.stringify(filtered));
    navigation.navigate('Dashboard');
  };

  return (
    <SafeAreaView style={{flex:1, backgroundColor:'#0b0b10'}}>
      <View style={{padding:16}}>
        <Text style={{color:'#fff', fontSize:22, fontWeight:'800'}}>{app.appName}</Text>
        <Text style={{color:'#9aa0b0', marginTop:4}}>Choose daily limit</Text>
      </View>
      <View style={{padding:16}}>
        <View style={{flexDirection:'row', flexWrap:'wrap'}}>
          {presets.map(p=> (
            <TouchableOpacity key={p} onPress={()=>setMinutes(p)} style={{backgroundColor: minutes===p ? '#6b9bff' : '#121218', padding:12, borderRadius:10, margin:6}}>
              <Text style={{color: minutes===p ? '#000' : '#fff'}}>{p} min</Text>
            </TouchableOpacity>
          ))}
        </View>
        <TouchableOpacity onPress={save} style={{backgroundColor:'#ff6b6b', padding:14, borderRadius:12, marginTop:24, alignItems:'center'}}>
          <Text style={{color:'#000', fontWeight:'800'}}>Start protecting</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}
