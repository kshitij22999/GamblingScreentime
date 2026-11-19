import { NativeModules, NativeEventEmitter } from 'react-native';

const { TimeGambler } = NativeModules as any;

export default {
  getInstalledApps: TimeGambler?.getInstalledApps || (()=>Promise.resolve([])),
  startMonitoring: TimeGambler?.startMonitoring || (()=>Promise.resolve()),
  stopMonitoring: TimeGambler?.stopMonitoring || (()=>Promise.resolve()),
  addListener: (cb: (event:any)=>void)=>{
    const emitter = new NativeEventEmitter(TimeGambler);
    return emitter.addListener('ForegroundChanged', cb);
  }
};
