import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import * as core from './core.mjs';

// Execute the real application state machine with small DOM/audio test doubles.
// This tests deferred speech/music cancellation without running a browser.
function harness({native=false,storedMute=false}={}) {
 const calls={play:0,pause:0,nativePlay:0,nativeListen:0,spoken:[],audioContexts:[]};
 const elements=new Map();
 function element(id='') {return {id,checked:true,value:'',textContent:'',hidden:false,muted:false,currentTime:0,duration:90,style:{},classList:{add(){}},children:[],
  getContext(){return {};},append(...nodes){this.children.push(...nodes)},replaceChildren(...nodes){this.children=[...nodes]},setAttribute(){},addEventListener(){},
  play(){calls.play++;return Promise.resolve();},pause(){calls.pause++;}};}
 const document={getElementById(id){if(!elements.has(id))elements.set(id,element(id));return elements.get(id);},createElement(){return element();},addEventListener(){},body:{classList:{add(){}}}};
 const store=new Map([['pleos-muted',JSON.stringify(storedMute)]]);
 const localStorage={getItem(k){return store.get(k)||null;},setItem(k,v){store.set(k,v)},removeItem(k){store.delete(k)}};
 class FakeAudioContext {
  constructor(){this.currentTime=0;this.nodes=[];this.destination=this.node('speaker');calls.audioContexts.push(this);}
  node(kind){const n={kind,connections:[],connect(target){this.connections.push(target)}};this.nodes.push(n);return n;}
  createMediaElementSource(element){const n=this.node('source');n.element=element;return n;}
  createMediaStreamDestination(){return this.node('recording');}
  createGain(){const n=this.node('gain');n.gain={value:1,cancelScheduledValues(){},setValueAtTime(value){this.value=value}};return n;}
  resume(){return Promise.resolve();}
  // Deliberately ignore HTMLMediaElement.muted: both sinks must be protected by
  // the Web Audio graph itself, even if each media source still emits a signal.
  sampleAt(kind){const walk=(node,level)=>{const output=level*(node.gain?.value??1);return node.kind===kind?output:node.connections.reduce((sum,next)=>sum+walk(next,output),0)};return this.nodes.filter(n=>n.kind==='source').reduce((sum,n)=>sum+walk(n,1),0);}
 }
 const window={AudioContext:FakeAudioContext,addEventListener(){},speechSynthesis:{cancel(){},speak(u){calls.spoken.push(u)}}};
 if(native)window.PleosNative={getStatus:()=>JSON.stringify({uxRestricted:false}),speak(){window.onPleosEvent?.({type:'music-pause'})},stopSpeaking(){},stopListening(){},pauseAudio(){window.onPleosEvent?.({type:'music-pause'})},playAudio(){calls.nativePlay++},listen(){calls.nativeListen++;window.onPleosEvent?.({type:'listening',active:true})}};
 const context=vm.createContext({...core,document,window,localStorage,console,setTimeout,clearTimeout,performance,Image:class{},SpeechSynthesisUtterance:class{constructor(text){this.text=text}},requestAnimationFrame(){}});
 const source=fs.readFileSync(new URL('./app.js',import.meta.url),'utf8').replace(/^import .*\n/,'');
 vm.runInContext(source+`\nthis.api={beginListening,cancelDemo,playTrack,applyMute,setupAudio,startSession,submit,playbackProgress,stopMusic,setSelected(){selected=TRACKS[0]},setMicWanted(value){micWanted=value},setMute(value){muted=value;applyMute()},state:()=>({auto,phase,muted,playing,talking,micWanted,listening,selected:selected?.id,previewIndex,tracks:tracks.map(t=>t.id)})};`,context);
 function clickChoice(label){const button=elements.get('choices').children.find(node=>node.textContent===label);assert.ok(button,`Rendered choice is missing: ${label}`);return button.onclick();}
 return {api:context.api,calls,elements,store,window,clickChoice};
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

test('recording audio graph mutes both speakers and saved audio without pausing the timeline',async()=>{
 const h=harness();await h.api.setupAudio();const graph=h.calls.audioContexts[0];
 assert.equal(graph.sampleAt('speaker'),2);assert.equal(graph.sampleAt('recording'),2);
 const pauses=h.calls.pause;h.api.setMute(true);
 assert.equal(graph.sampleAt('speaker'),0);assert.equal(graph.sampleAt('recording'),0);assert.equal(h.calls.pause,pauses);
 await h.api.setupAudio();assert.equal(h.calls.audioContexts.length,1);assert.equal(graph.sampleAt('speaker'),0);assert.equal(graph.sampleAt('recording'),0);
 h.api.setMute(false);assert.equal(graph.sampleAt('speaker'),2);assert.equal(graph.sampleAt('recording'),2);assert.equal(h.calls.play,0);
});
test('recording created with saved mute starts with zero output on both sinks',async()=>{
 const h=harness({storedMute:true});await h.api.setupAudio();const graph=h.calls.audioContexts[0];
 assert.equal(graph.sampleAt('speaker'),0);assert.equal(graph.sampleAt('recording'),0);assert.equal(h.calls.play,0);
});

test('normal native music-pause during first question preserves automatic listening',async()=>{
 const h=harness({native:true});h.api.setMicWanted(true);h.api.startSession();
 // The native double emits music-pause from both pauseAudio() and speak(),
 // even when no song is playing, matching the actual native event contract.
 assert.equal(h.api.state().micWanted,true);h.window.onPleosEvent({type:'speech-start'});
 h.window.onPleosEvent({type:'speech-end',stopped:false});await new Promise(resolve=>setTimeout(resolve,400));
 assert.equal(h.calls.nativeListen,1);assert.equal(h.api.state().listening,true);assert.equal(h.api.state().micWanted,true);
 h.api.setMicWanted(false);h.api.cancelDemo();
});
for(const type of ['audio-focus-lost','suspended'])test(`native ${type} still stops the conversation`,()=>{
 const h=harness({native:true});h.api.setMicWanted(true);h.window.onPleosEvent({type});
 assert.equal(h.api.state().micWanted,false);assert.equal(h.api.state().listening,false);
});

test('native timeline uses measured progress, survives pause and resets for a new play request',async()=>{
 const h=harness({native:true});h.elements.get('music').currentTime=77;h.elements.get('music').duration=123;
 h.window.onPleosEvent({type:'music-progress',positionMs:12450,durationMs:90123});
 assert.equal(h.api.playbackProgress().position,12.45);assert.equal(h.api.playbackProgress().duration,90.123);
 h.window.onPleosEvent({type:'music-pause'});h.api.stopMusic();assert.equal(h.api.playbackProgress().position,12.45);
 await h.api.playTrack(core.TRACKS[1],false);assert.equal(h.api.playbackProgress().position,0);assert.equal(h.api.playbackProgress().duration,0);
 h.window.onPleosEvent({type:'music-progress',positionMs:2300,durationMs:91000});assert.equal(h.api.playbackProgress().position,2.3);
});
test('web timeline keeps reading HTML media time rather than native events',()=>{
 const h=harness();h.elements.get('music').currentTime=27;h.elements.get('music').duration=90;
 h.window.onPleosEvent({type:'music-progress',positionMs:5000,durationMs:20000});
 assert.equal(h.api.playbackProgress().position,27);assert.equal(h.api.playbackProgress().duration,90);
});

// These tests execute app.js button handlers with fake DOM/media only. They are
// conversation-branch unit tests, not evidence of actual SDK/audio execution.
const settleHandlers=()=>new Promise(resolve=>setImmediate(resolve));
async function completeExampleOnboarding(h){
 h.elements.get('voice').checked=false;
 h.elements.get('start').onclick();assert.equal(h.api.state().phase,'age');
 for(const [label,next] of [
  ['30대','gender'],['말하지 않기','taste'],
  ['피아노 연주곡을 즐겨 들어요','tastePick'],['1. Reverie','preview'],
  ['이 곡으로 감상하기','state'],
 ]){
  h.clickChoice(label);await settleHandlers();assert.equal(h.api.state().phase,next,label);
 }
}
for(const native of [false,true])test(`complete example-button journey through ${native?'mocked native bridge':'mocked web media'} saves and reopens the diary`,async()=>{
 const h=harness({native});
 try{
  await completeExampleOnboarding(h);
  assert.equal(JSON.parse(h.store.get('pleos-profile')).favorite,'reverie');
  for(const [label,next] of [
   ['오늘 일이 많아서 피곤해요','emotion'],
   ['살짝 슬픈 것 같아요','need'],
   ['휴식이 필요해요','body'],
   ['등에서 따스함이 느껴져요','recommend'],
   ['1. Reverie','preview'],
   ['이 곡으로 감상하기','listening'],
  ]){
   h.clickChoice(label);await settleHandlers();assert.equal(h.api.state().phase,next,label);
  }
  // One taste preview, one session preview, and one full-listening request.
  assert.equal(native?h.calls.nativePlay:h.calls.play,3);
  h.clickChoice('감상 마치기');assert.equal(h.api.state().phase,'imagery');
  h.clickChoice('포근한 이불의 느낌이 생각났어요');assert.equal(h.api.state().phase,'image');
  h.clickChoice('이 이미지로 일기 저장');assert.equal(h.api.state().phase,'saved');
  const entries=JSON.parse(h.store.get('pleos-diaries'));
  assert.equal(entries.length,1);
  assert.equal(entries[0].emotion,'살짝 슬픈 것 같아요');
  assert.equal(entries[0].need,'휴식이 필요해요');
  assert.equal(entries[0].body,'등에서 따스함이 느껴져요');
  assert.equal(entries[0].imagery,'포근한 이불의 느낌이 생각났어요');
  assert.equal(entries[0].track,'Reverie');assert.equal(entries[0].image,true);
  assert.equal(entries[0].date,new Date().toLocaleDateString('ko-KR'));
  h.clickChoice('일기 다시 보기');assert.equal(h.api.state().phase,'diaries');
  assert.equal(h.api.state().selected,'reverie');
  assert.match(h.elements.get('screen-reader').textContent,/포근한 이불의 느낌이 생각났어요/);
 }finally{h.api.cancelDemo();h.api.stopMusic();}
});
test('already stated fear, frustration and tension ask for a need without asking emotion again',async()=>{
 const h=harness();
 try{
  await completeExampleOnboarding(h);
  for(const answer of ['두렵네요','두려워요','짜증이 나요','답답해요','긴장돼요','살짝 슬픈 것 같아요']){
   h.api.startSession();assert.equal(h.api.state().phase,'state');
   h.api.submit(answer);assert.equal(h.api.state().phase,'need',answer);
   assert.ok(h.elements.get('choices').children.some(button=>button.textContent==='휴식이 필요해요'));
  }
 }finally{h.api.cancelDemo();h.api.stopMusic();}
});
