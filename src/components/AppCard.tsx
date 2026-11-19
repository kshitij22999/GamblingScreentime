import React from 'react';
import {View, Text, TouchableOpacity, Image} from 'react-native';

export default function AppCard({app}:{app:any}){
  return (
    <View style={{backgroundColor:'#121218', padding:16, borderRadius:12, marginBottom:12}}>
      <View style={{flexDirection:'row', alignItems:'center'}}>
        <View style={{width:48, height:48, borderRadius:8, backgroundColor:'#1a1a1f', justifyContent:'center', alignItems:'center'}}>
          {app.icon ? <Image source={{uri:app.icon}} style={{width:40,height:40}} /> : <Text style={{color:'#fff'}}>{app.appName?.charAt(0)}</Text>}
        </View>
        <View style={{marginLeft:12, flex:1}}>
          <Text style={{color:'#fff', fontWeight:'700'}}>{app.appName}</Text>
          <Text style={{color:'#9aa0b0'}}>{Math.floor((app.remainingSeconds||0)/60)} min remaining</Text>
        </View>
        <TouchableOpacity style={{backgroundColor:'#6b9bff', paddingHorizontal:12, paddingVertical:8, borderRadius:8}}>
          <Text style={{color:'#000', fontWeight:'700'}}>🃏 GAMBLE</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
