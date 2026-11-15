import test from 'node:test';import assert from 'node:assert/strict';import {selectionIntent,negative,emotion,reflect,recommend,canShowDiary,imageMatches} from './core.mjs';
test('ordinary Korean selection phrases are accepted',()=>{assert.deepEqual(selectionIntent('두 번째 곡을 선택할게요.'),{type:'choose',index:1});assert.deepEqual(selectionIntent('두번째 곡 추천해 줘'),{type:'preview',index:1});assert.deepEqual(selectionIntent('다른 곡을 들을래.'),{type:'other'});assert.deepEqual(selectionIntent('이 곡이 좋겠어요'),{type:'choose'});assert.deepEqual(selectionIntent('이번 곡이 좋아요'),{type:'choose'});assert.equal(selectionIntent('잘 모르겠어요').type,'unknown');});
test('negative state goes through desired feeling instead of matching distress',()=>{assert(negative('화가 나요'));assert(emotion('무서워요'));assert(negative('슬퍼요'));const tracks=recommend({music:'피아노'}, {emotion:'불안해요',need:'휴식과 편안함',body:'따뜻함'});assert.equal(tracks[0].id,'reverie');});
test('wide low sensation ranks cello differently from piano comfort',()=>{assert.equal(recommend({music:'첼로'}, {need:'만족',body:'넓게 퍼지는 묵직한 느낌'})[0].id,'hiraeth');});
test('unknown and driving vehicle states cannot display diary',()=>{assert.equal(canShowDiary(null),false);assert.equal(canShowDiary(false),false);assert.equal(canShowDiary(true),true);});
test('static comforter illustration is never assigned to unrelated imagery',()=>{assert(imageMatches('포근한 이불의 느낌'));assert.equal(imageMatches('넓은 들판에 새들이 날아가요'),false);});
test('the exact sad example button and clear sadness forms are recognized consistently',()=>{
 for(const text of ['살짝 슬픈 것 같아요','슬프네요','슬퍼요','슬픔이 느껴져요']){
  assert.equal(negative(text),true,text);assert.equal(emotion(text),true,text);
  assert.equal(reflect(text),'슬픈 마음이 드시는군요.',text);
 }
});
test('every recognized negative feeling is also an already stated emotion',()=>{
 for(const text of ['불안해요','초조해요','무섭네요','무서워요','두렵네요','두려워요','화가 나요','분노가 느껴져요','짜증이 나요','우울해요','걱정돼요','답답해요','긴장돼요']){
  assert.equal(negative(text),true,text);assert.equal(emotion(text),true,text);
 }
 assert.equal(negative('편안해요'),false);assert.equal(emotion('편안해요'),true);
 assert.equal(emotion('오늘 일이 많아서 피곤해요'),false);
});
