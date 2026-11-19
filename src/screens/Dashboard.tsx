import React, {useEffect, useState} from 'react';
import {View, Text, FlatList, TouchableOpacity, SafeAreaView} from 'react-native';
import AppCard from '../components/AppCard';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {useNavigation} from '@react-navigation/native';

export default function Dashboard(){
  const [apps, setApps] = useState<any[]>([]);
  const nav = useNavigation();

  useEffect(()=>{
    (async ()=>{
      const raw = await AsyncStorage.getItem('protectedApps');
      setApps(raw ? JSON.parse(raw) : []);
    })();
  },[]);

  return (
    <SafeAreaView style={{flex:1, backgroundColor:'#0b0b10'}}>
      <View style={{padding:16}}>
        <Text style={{color:'#fff', fontSize:28, fontWeight:'800'}}>TIME GAMBLER</Text>
        <Text style={{color:'#9aa0b0', marginTop:4}}>Play responsibly. Scroll irresponsibly.</Text>
      </View>
      <FlatList
        data={apps}
        keyExtractor={(i:any)=>i.packageName}
        contentContainerStyle={{padding:16}}
        renderItem={({item})=> <AppCard app={item} />}
        ListEmptyComponent={() => (
          <View style={{padding:20}}>
            <Text style={{color:'#c0c6d0'}}>No protected apps yet.</Text>
            <TouchableOpacity onPress={()=>nav.navigate('AppSelection' as never)} style={{marginTop:12}}>
              <Text style={{color:'#6b9bff'}}>Add an app</Text>
            </TouchableOpacity>
          </View>
        )}
      />
    </SafeAreaView>
  );
}
