import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import * as core from './core.mjs';

// Execute the real application state machine with small DOM/audio test doubles.
// This tests deferred speech/music cancellation without running a browser.
function harness({native=false,storedMute=false}={}) {
 const calls={play:0,pause:0,nativePlay:0,spoken:[]};
 const elements=new Map();
 function element(id='') {return {id,checked:true,value:'',textContent:'',hidden:false,muted:false,currentTime:0,duration:90,style:{},classList:{add(){}},
  getContext(){return {};},append(){},replaceChildren(){},setAttribute(){},addEventListener(){},
  play(){calls.play++;return Promise.resolve();},pause(){calls.pause++;}};}
 const document={getElementById(id){if(!elements.has(id))elements.set(id,element(id));return elements.get(id);},createElement(){return element();},addEventListener(){},body:{classList:{add(){}}}};
 const store=new Map([['pleos-muted',JSON.stringify(storedMute)]]);
 const localStorage={getItem(k){return store.get(k)||null;},setItem(k,v){store.set(k,v)},removeItem(k){store.delete(k)}};
 const window={addEventListener(){},speechSynthesis:{cancel(){},speak(u){calls.spoken.push(u)}}};
 if(native)window.PleosNative={getStatus:()=>JSON.stringify({uxRestricted:false}),speak(){},stopSpeaking(){},stopListening(){},pauseAudio(){},playAudio(){calls.nativePlay++},listen(){}};
 const context=vm.createContext({...core,document,window,localStorage,console,setTimeout,clearTimeout,performance,Image:class{},SpeechSynthesisUtterance:class{constructor(text){this.text=text}},requestAnimationFrame(){}});
 const source=fs.readFileSync(new URL('./app.js',import.meta.url),'utf8').replace(/^import .*\n/,'');
 vm.runInContext(source+`\nthis.api={beginListening,cancelDemo,playTrack,applyMute,startSession,submit,setSelected(){selected=TRACKS[0]},setMute(value){muted=value;applyMute()},state:()=>({auto,phase,muted,playing,talking,selected:selected?.id,previewIndex,tracks:tracks.map(t=>t.id)})};`,context);
 return {api:context.api,calls,elements,store,window};
}
for(const native of [false,true]) {
 test(`stopping during guidance never starts delayed ${native?'native':'web'} music`,async()=>{
  const h=harness({native});h.api.setSelected();const pending=h.api.beginListening('이 곡이 좋겠어요');
  h.api.cancelDemo();await pending;
  assert.equal(h.calls.play,0);assert.equal(h.calls.nativePlay,0);
 });
}
test('master mute survives bootstrap and all later web playback',async()=>{
 const h=harness({storedMute:true});assert.equal(h.elements.get('music').muted,true);assert.equal(h.elements.get('narration').muted,true);
 await h.api.playTrack(core.TRACKS[0],false);assert.equal(h.elements.get('music').muted,true);
 h.api.setMute(false);assert.equal(h.elements.get('music').muted,false);assert.equal(h.store.get('pleos-muted'),'false');
});
test('native music is not started while master mute is active',async()=>{
 const h=harness({native:true,storedMute:true});await h.api.playTrack(core.TRACKS[0],false);assert.equal(h.calls.nativePlay,0);
});

test('onboarding preference is never carried into the reordered session recommendation',async()=>{
 const h=harness();h.api.startSession();h.api.submit('30대');h.api.submit('말하지 않기');h.api.submit('피아노와 첼로를 좋아해요');
 h.api.submit('두 번째 곡을 들어볼래요');await Promise.resolve();h.api.submit('이 곡이 좋겠어요');
 await new Promise(resolve=>setImmediate(resolve));assert.equal(h.api.state().playing,false);assert.equal(h.api.state().phase,'state');assert.equal(h.api.state().previewIndex,null);assert.equal(h.api.state().selected,undefined);
 h.api.submit('불안해요');h.api.submit('편안해지고 싶어요');h.api.submit('넓게 퍼지는 묵직한 느낌이에요');
 assert.equal(h.api.state().phase,'recommend');assert.equal(h.api.state().tracks[0],'hiraeth');assert.equal(h.api.state().previewIndex,null);assert.equal(h.api.state().selected,undefined);
 h.api.submit('이 곡이 좋겠어요');assert.equal(h.api.state().phase,'recommend');
});

test('a vehicle telemetry error does not terminate an independent voice turn',async()=>{
 const h=harness({native:true});h.api.setSelected();const pending=h.api.beginListening('이 곡이 좋겠어요');
 assert.equal(h.api.state().talking,true);h.window.onPleosEvent({type:'error',component:'vehicle-capability',code:'UNAVAILABLE'});
 assert.equal(h.api.state().talking,true);assert.equal(h.calls.nativePlay,0);h.api.cancelDemo();await pending;
});
