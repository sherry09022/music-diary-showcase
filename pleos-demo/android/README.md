# 음악일기 · Pleos Connect 네이티브 데모

기존 iPhone 앱과 별개의 Android Automotive 평가용 앱입니다. 웹 데모와 같은 화면 파일을 APK에 포함하고, 네이티브에서 **실제 Pleos SDK**를 호출합니다. 웹사이트에서 이 SDK가 실행되는 것은 아닙니다.

## 구현 범위

| 기능 | 네이티브 구현 | 검증 상태 |
| --- | --- | --- |
| 음성 합성 | TextToSpeech 3.3.1, `Mode.ON_DEVICE` | 실제 에뮬레이터에서 초기화 및 `ko-KR` PCM 생성·AudioTrack 기록 완료 확인. 물리적 소리 출력은 미검증 |
| 음성 인식 | SpeechToText 3.3.1, `Mode.ON_DEVICE`, 중간·최종 텍스트 전달 | 실제 SDK 초기화 확인. 마이크 음성 인식은 미검증 |
| 차량 정보 | Vehicle 2.3.2, capability 확인 후 브레이크 페달 조회 | 실제 에뮬레이터 서비스 연결 확인. 실차 동작은 미검증 |
| 화면 제한 | 표준 AAOS `CarUxRestrictionsManager` 조회·변경 구독 | 실제 에뮬레이터에서 `uxRestricted: false`, `activeRestrictions: 0` 확인 |
| 음악·일기 | Android MediaPlayer와 WebView 일기 저장 | 실제 OS에서 미리듣기·본 감상의 플레이어 시간 증가와 오디오 활성 상태, 심상 일기 저장·재열기 확인. 가청 출력은 미검증 |
| 빌드 | GitHub Actions에서 `assembleDebug` + `lintDebug` | APK 소스 `75d7004` [실행 성공](https://github.com/sherry09022/music-diary-showcase/actions/runs/43728649023), 웹 회귀 23개 통과 |

공개 debug APK의 소스는 `75d7004411fb41ad5e649d74618187729a848773`이며, 파일은 33,993,137바이트입니다. SHA-256은 `cbd2e5c1092ef56f60150fae2402519277d218ce8619571a4de438caf8a3a184`입니다. Lint는 오류 0개·경고 10개로 통과했습니다. 이 빌드와 실제 실행 검증의 커밋·범위는 [검증 기록](../downloads/build-verification.json)에 각각 기록하며, 이후 APK에 같은 해시를 적용하지 않습니다.

**브레이크 페달을 밟았다는 값으로 주차 상태를 추측하지 않습니다.** 이 SDK 버전의 공개 Brake 인터페이스에는 주차 브레이크 조회가 없습니다. `parked`는 항상 `null`로 전달합니다. 폼·일기 화면 허용 여부는 별도로 연결한 표준 AAOS `CarUxRestrictionsManager`의 `isRequiresDistractionOptimization()` 결과를 따릅니다. `uxRestricted === false`일 때만 전체 화면을 허용하고, 연결 전·오류·연결 해제 상태는 `null`로 보내 제한을 유지합니다. 제한이 켜지면 `activeRestrictions`의 NO_KEYBOARD / NO_SETUP / NO_TEXT_MESSAGE / NO_VOICE_TRANSCRIPTION 등 비트도 준수해야 합니다. 이 앱은 실제 차량에서 운전자 주의 분산 검증을 마치지 않았으며 manifest에 distractionOptimized로 승인된 것처럼 선언하지 않았습니다. 이동 중 OS가 앱 화면을 차단할 수 있습니다.

ON_DEVICE 모드는 외부 서버 키를 넣지 않습니다. 서비스가 없거나 권한이 거부되면 오류를 전달하고, 브라우저 음성 엔진으로 조용히 바꾸거나 연결됐다고 표시하지 않습니다. SDK 의존성 자체는 공식 공개 Maven 저장소에서 다운로드합니다. SDK AAR 원본과 시스템 이미지는 재배포하지 않으며, 설치용 APK에는 필요한 SDK 라이브러리가 포함됩니다.

## 빌드

JDK 17, Android SDK Platform 35, Android SDK Build Tools 35.0.0을 준비합니다. 실행 대상은 공식 안내의 **Pleos Connect Emulator (Android API 34)** 입니다. 일반 Android 에뮬레이터는 Pleos 음성·차량 서비스를 제공하지 않습니다.

```bash
cd pleos-demo/android
./gradlew :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ai.projectshu.musicdiary.pleosdemo/.MainActivity
```

앱은 `../index.html`, `../app.js`, `../core.mjs`, `../styles.css`, `../assets/`를 빌드 시 복사합니다. 웹 파일을 별도로 복제·수정할 필요가 없습니다. MP4/WebM 시연 영상은 APK에서 제외합니다.

공개 저장소의 **Actions → Pleos native demo → Run workflow**에서도 빌드할 수 있습니다. 성공한 실행의 `music-diary-pleos-debug` 아티팩트에 APK와 lint 보고서가 들어갑니다. CI 컴파일 성공은 실제 차량 실행 성공을 뜻하지 않습니다.

## Pleos 에뮬레이터에서 확인할 내용

1. 공식 문서대로 Pleos API 34 시스템 이미지를 설치하고 Cold Boot합니다. 마이크 입력을 호스트에 연결합니다.
2. APK 설치 후 앱 권한에서 마이크를 허용합니다. Pleos STT/TTS 서비스 및 차량 읽기 권한을 확인합니다.
3. 앱 진단에서 `ttsReady`, `sttReady`가 실제 초기화 결과로 `true`가 되는지 확인합니다.
4. 질문이 끝난 뒤 말하고 중간 텍스트와 최종 텍스트가 모두 전달되는지 확인합니다.
5. 음악을 재생하면 인식이 멈추고, TTS 종료 콜백이 음악 오디오 포커스를 해제하지 않는지 확인합니다.
6. 차량 capability 실패·마이크 거절·서비스 미설치 때 미연결 상태와 복구 안내를 확인합니다.
7. 앱을 백그라운드로 보내면 마이크·음악·TTS가 멈추는지 확인합니다.

공개 GitHub Actions의 [실제 실행 43728653191](https://github.com/sherry09022/music-diary-showcase/actions/runs/43728653191), 커밋 `75d7004411fb41ad5e649d74618187729a848773`에서 공식 Pleos OS와 앱 실행, TTS/STT/Vehicle 준비 및 AAOS UX 허용 상태를 확인했습니다. 실제 접근성 버튼으로 온보딩 → 슬픔 → 필요한 휴식 → 몸의 감각 → 추천·미리듣기 → 본 감상 → 심상 → 일기 저장·재열기까지 통과했습니다. [전체 무음 녹화](../watch.html#native)와 [계측 결과 원문](../downloads/native-journey-verification.json)을 제공합니다.

본 감상에서는 앱 자체 MediaPlayer의 재생 상태와 시간 증가를 검사했으며, 체크포인트에 `musicPlaying: true`, `positionMs: 15725`, `durationMs: 90044`가 기록됐습니다. 오디오 스트림 활성 상태도 확인했습니다. 심상은 미리 제작한 이불 예시 이미지이며, 사용자 예시 답변·곡과 함께 저장한 일기 1개를 다시 열었습니다. 이 결과와 한국어 ON_DEVICE PCM 생성·AudioTrack 기록 확인은 실제 스피커로 들리는 소리의 검증과 구분합니다. 실제 마이크 인식·물리 스피커 출력·실차 동작 및 차량 배포 승인은 미검증입니다.

## 웹 브리지 계약

`window.PleosNative`가 있는 경우에만 네이티브 모드입니다.

```js
const status = JSON.parse(PleosNative.getStatus());
PleosNative.speak('지금 어떤 상태인가요?');
// speech-end 이후, 음악이 재생 중이지 않을 때 호출합니다.
PleosNative.listen();
PleosNative.stopListening();
PleosNative.stopSpeaking();
PleosNative.playAudio('assets/example.mp3');
PleosNative.pauseAudio();
PleosNative.stopAudio();
PleosNative.refreshVehicle();
window.onPleosEvent = (event) => {
  // ready, speech-start, speech-end, listening, transcript, vehicle, ux-restrictions,
  // music-start, music-pause, music-end, audio-focus-lost, suspended, error
};
```

`transcript` 이벤트의 `text`는 인식 결과, `final`은 SDK의 결과 완료 여부입니다. `listening.active=false`는 인식 종료를 뜻하며 사용자 답변 완료로 간주해서는 안 됩니다. 5초 응답 대기는 웹 대화 상태기계에서 관리합니다. `error`에는 `component`, `code`가 들어갑니다. 네이티브 브리지는 로컬 `appassets.androidplatform.net/assets/`에만 노출하며 원격 탐색·외부 리소스·파일 경로 접근을 차단합니다. 인식 문장은 Logcat/서버로 전송하지 않습니다.

`getStatus()`와 `ready`에는 `uxRestricted`, `activeRestrictions`가 포함됩니다. `ux-restrictions` 이벤트는 `restricted`, `activeRestrictions`, `source: 'aaos-car-service'`를 전달합니다. 제한이 없는 상태를 앱에서 임의로 만들지 않습니다.

## 근거 문서

- [Pleos Connect SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)
- [에뮬레이터 구성](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [TTS 초기화](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release), [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions)
- [STT 이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners), [인식 시작·종료](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions)
- [Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [Brake 인터페이스](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/brake/entry)
- [표준 AAOS UX restrictions](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions), [manager](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager)
