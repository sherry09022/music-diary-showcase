export const TRACKS = [
 {id:'reverie',title:'Reverie',artist:'Scott Buckley',color:'#adbdff',texture:'피아노 · 잔잔하고 부드러운 흐름',tags:['피아노','휴식','편안','따뜻','따스','이완'],file:'assets/reverie.mp3'},
 {id:'hiraeth',title:'Hiraeth',artist:'Scott Buckley',color:'#78c6c8',texture:'첼로 · 낮고 넓게 퍼지는 울림',tags:['첼로','현악','넓','만족','묵직','깊'],file:'assets/hiraeth.mp3'},
 {id:'filaments',title:'Filaments',artist:'Scott Buckley',color:'#d6b78c',texture:'피아노와 현악 · 섬세한 여운',tags:['현악','피아노','슬픔','위로','그리움'],file:'assets/filaments.mp3'},
];
export function negative(text){return /불안|초조|슬프|슬퍼|슬픔|슬픈|무섭|무서|두렵|두려|화가|분노|짜증|우울|걱정|답답|긴장/.test(text);}
export function emotion(text){return negative(text)||/기쁘|기뻐|행복|편안|만족|설레|즐거/.test(text);}
export function reflect(text){
 if(/화가|분노/.test(text))return '화가 나시는군요.';
 if(/무섭|무서|두렵|두려/.test(text))return '무서운 마음이 드시는군요.';
 if(/슬프|슬퍼|슬픔|슬픈/.test(text))return '슬픈 마음이 드시는군요.';
 if(/불안|초조/.test(text))return '마음이 불안하고 조급하시군요.';
 if(/피곤|지쳤|지쳐/.test(text))return '많이 피곤하셨군요.';
 if(/편안/.test(text))return '편안함을 느끼고 계시는군요.';
 if(/만족/.test(text))return '만족스러운 느낌이 드시는군요.';
 if(/배가?\s*불/.test(text))return '배가 부르시는군요.';
 return '말씀하신 느낌을 함께 살펴볼게요.';
}
export function selectionIntent(text){
 const s=text.replace(/\s|[.,!?。]/g,'');
 if(/다른곡|다음곡|다른음악|다시추천|더추천|싫어|아니요|아니야/.test(s))return {type:'other'};
 const patterns=[/첫번째|첫째|1번|일번|첫곡/,/두번째|둘째|2번|이번째/,/세번째|셋째|3번|삼번/];
 for(let i=0;i<patterns.length;i++)if(patterns[i].test(s))return {type:/선택|이걸|고를|결정/.test(s)?'choose':'preview',index:i};
 if(/이곡|이번곡|지금곡|현재곡|이음악|이거|좋겠|좋아|들을게|듣겠|네$|예$|응$/.test(s))return {type:'choose'};
 return {type:'unknown'};
}
export function recommend(profile,session){
 const wanted=`${session.need||session.emotion||''} ${session.body||''}`;
 const taste=`${profile.music||''} ${profile.taste||''}`;
 return TRACKS.map((track,index)=>({track,score:track.tags.reduce((n,tag)=>n+(wanted.includes(tag)?4:0)+(taste.includes(tag)?2:0),0),index})).sort((a,b)=>b.score-a.score||a.index-b.index).map(x=>x.track);
}
export function imageMatches(text){return /이불|담요/.test(text);}
export function canShowDiary(parked){return parked===true;}
