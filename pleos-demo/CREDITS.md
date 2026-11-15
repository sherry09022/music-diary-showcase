# 데모 음원·이미지·기술 출처

## 실제 재생되는 음악

아래 곡들은 작곡가의 공식 Creative Commons 라이브러리에서 제공되는 원본입니다. 각각 90초를 발췌하여 시작 1초·끝 3초 페이드를 적용하고 128 kbps MP3로 변환했습니다. 가사가 없는 피아노·현악·앰비언트 음악을 사용합니다.

- **‘Reverie’ by Scott Buckley — released under CC-BY 4.0. www.scottbuckley.com.au**
  [공식 곡·라이선스](https://www.scottbuckley.com.au/library/reverie/) · 원본 00:50–02:20 발췌 · `assets/reverie.mp3`
- **‘Hiraeth’ by Scott Buckley — released under CC-BY 4.0. www.scottbuckley.com.au**
  [공식 곡·라이선스](https://www.scottbuckley.com.au/library/hiraeth/) · 원본 00:35–02:05 발췌 · `assets/hiraeth.mp3`
- **‘Filaments’ by Scott Buckley — released under CC-BY 4.0. www.scottbuckley.com.au**
  [공식 곡·라이선스](https://www.scottbuckley.com.au/library/filaments/) · 원본 00:35–02:05 발췌 · `assets/filaments.mp3`

[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/). 위 크레딧과 곡별 출처·라이선스·편집 표시를 배포물과 녹화 영상 설명에 유지해 주세요. 이 공개 데모에는 ‘Kiss the Rain’ 음원을 배포하지 않습니다.

## 그림과 음성

- `assets/comforter.jpg`: 이 프로젝트를 위해 AI로 제작한 가상의 인물·이불·비 오는 창가의 예시 그림. 사용자 지시 ‘음악을 듣고 포근한 이불의 느낌이 생각났어’를 참고해 미리 제작했습니다. 데모 안에서 사용자의 임의 문장을 이미지 생성 API로 전송하지 않습니다.
- `assets/voice-*.mp3`: 데모 안내 문장을 macOS Yuna 음성으로 합성한 시연용 파일. 실제 Pleos SDK 또는 ChatGPT 음성의 녹음이 아닙니다. 네이티브 대화 모드는 Pleos ON_DEVICE TTS를 직접 호출합니다.
- 화면·곡 선택·진단·대화 시나리오는 Project SHU 음악일기 별도 데모용 코드입니다. 실제 차량 계기판이나 Hyundai/Pleos 로고를 복제하지 않습니다.

## SDK 문서

- [Pleos Connect SDK 개요](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)
- [Pleos 에뮬레이터 구성](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [TextToSpeech SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)
- [SpeechToText SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)
- [Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
- [Android CarUxRestrictions](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions)

공식 문서 확인일: 2026-11-15. SDK는 공식 Maven 저장소에서 빌드 시 내려받으며 이 공개 저장소에 바이너리를 복사하지 않습니다.
