# Pleos 실제 에뮬레이터 실험

`.github/workflows/pleos-emulator.yml`은 공개 GitHub 저장소의 무료 `ubuntu-24.04` runner에서 **공식 Pleos API 34 x86_64 시스템 이미지**를 설치하고 앱을 실행해 보는 수동 실험입니다. 기존 APK 컴파일 workflow와 별개입니다. 브라우저 시뮬레이터를 Pleos 실행 결과로 사용하지 않습니다.

## 실행 전 SDK 계약 검토

[공식 이미지 저장소 XML](https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml)에 `pleos-connect-sdk-license` 전문이 있습니다. XML의 **2.1**은 SDK 사용 전 계약 수락을 요구하고, **2.2**는 설치·사용도 계약 수락으로 취급합니다. 계약 전체를 검토하고 해당 사용에 동의할 권한이 있는 사람이 실행해야 합니다.

Actions → **Pleos emulator experiment (silent)** → Run workflow에서 `acknowledge_pleos_license`를 명시적으로 체크해야 합니다. 기본값은 `false`이며 체크하지 않으면 **어떤 SDK 설치보다 먼저 실패**합니다. 저장소에 workflow를 추가하거나 소스를 push해도 자동으로 실행되지 않습니다. 이 파일의 준비는 사용자 대신 계약에 동의했다는 뜻이 아닙니다.

동의한 실행에서만 `sdkmanager` 설치 프롬프트에 수락을 전달합니다. 라이선스 파일을 위조하거나, 다운로드가 공개돼 있다는 이유로 계약이 없다고 취급하지 않습니다. 기본 Android 도구에도 해당 Android SDK 라이선스가 적용됩니다. [Android의 CI 라이선스 안내](https://developer.android.com/tools/sdkmanager#accept-licenses)를 확인하세요.

## 실험 조건과 한계

- [공식 Pleos 설치 문서](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)가 공개한 Linux 저장소만 사용합니다. `sdkmanager`에서 `system-images;android-34;pleos_car_emulator;x86_64`가 발견되지 않으면 중단하며 일반 Android 이미지로 바꾸지 않습니다.
- [Pleos 설정 문서](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)는 외장 GPU를 요구합니다. 무료 runner의 **소프트웨어 렌더링 지원은 Pleos가 보장하지 않습니다.** 설치 실패, 부팅 실패, 검은 화면도 실험 결과로 남깁니다.
- Android의 현재 지원 옵션인 [`-gpu swiftshader`](https://developer.android.com/studio/run/emulator-acceleration)와 `-no-window`를 사용합니다. `swiftshader_indirect`는 emulator 36.4.9부터 deprecated입니다. `-no-snapshot`으로 cold boot하고, KVM이 없으면 중단합니다.
- ZIP만 약 4.09GB이며 압축 해제와 빌드에 추가 디스크가 필요합니다. runner 공간이 부족하면 설치가 실패할 수 있습니다. 유료 GPU·larger runner로 자동 전환하지 않습니다.
- 새로 생성한 AVD에서 저장소의 해당 커밋을 빌드·설치합니다. 개인 계정에 로그인하지 않으며 사용자 일기나 실제 마이크 입력을 넣지 않습니다.
- `-no-audio`로 음소거합니다. 음악·마이크·TTS의 실제 소리는 검증하지 않습니다. [Android screenrecord](https://developer.android.com/tools/adb#screenrecord)도 오디오를 녹음하지 않습니다.

## 실행 단계와 통과 조건

이 workflow는 **공식 Pleos OS 부팅 → 네이티브 앱 시작 → 실제 SDK 준비 확인 → 접근성 버튼을 이용한 전체 UI 여정**을 순서대로 시도합니다. 아래는 실행 절차이며, 특정 실행의 성공을 미리 선언하는 내용이 아닙니다. 실제 결과는 해당 실행의 증거 파일로 판단합니다.

1. **OS 부팅:** `sys.boot_completed.txt`가 `1`이어야 부팅 완료입니다. `adb devices`에 나타나는 것만으로 성공하지 않습니다.
2. **앱 시작:** APK를 설치하고 실제 `MainActivity`를 시작합니다. 시작 화면과 35초 이내의 무음 영상을 캡처합니다. 검은 화면·OS 차단 화면·앱 크래시는 사람이 확인해야 합니다.
3. **SDK 준비:** 디버그 WebView가 제공하는 실제 `PleosNative.getStatus()`를 최대 60초 동안 읽습니다. `transport`가 `pleos-native`, `ttsReady`, `sttReady`, `vehicleConnected`가 모두 `true`이고 `uxRestricted`가 실제 boolean이어야 이 단계가 통과합니다. 필요한 조건이 갖춰졌지만 차량 연결만 준비되지 않았을 때 실제 `refreshVehicle()`을 한 번 요청할 수 있습니다. 실패 값을 수정하거나 SDK·차량 값을 모의 값으로 바꾸지 않습니다.
4. **전체 UI 여정:** SDK 준비 단계가 성공한 경우에만 계측 APK를 실행합니다. 계측테스트는 실제 SDK 준비 상태에 더해 **`uxRestricted === false`**를 요구합니다. 각 버튼 조작과 감상 대기 중에도 이 조건을 다시 확인하고, 상태가 불명확하거나 제한이 켜지면 중단합니다. 준비 단계의 `uxRestricted: true`는 SDK 상태 확인에는 유효해도 UI 여정 통과 조건은 아닙니다.
5. **결과 판정:** 계측 프로세스가 정상 종료되고 `journey-evidence/result.json`의 `passed`가 `true`이며, 계측 로그에 실패 표시가 없고 무음 영상 파일이 존재해야 `journey-verification.json`의 `passed`가 `true`가 됩니다. 파일이 존재한다는 사실만으로 화면 내용이 올바르다고 판단하지 않으며, 시각 검토는 별도로 필요합니다.

### 실제 UI 여정

`PleosJourneyTest.parkedNativeJourneyWithExampleAnswers()`는 UIAutomator로 화면의 **실제 접근성 버튼을 누르고 필요한 경우 WebView를 스크롤**합니다. DOM 조작, JavaScript 앱 상태 대입, SDK 모의 처리, VHAL·차량 상태 변경을 사용하지 않습니다.

- 직접 체험 → 30대 → 성별 말하지 않기 → 평소 피아노 취향 입력
- 첫 곡 맛보기 → 취향곡 확정
- 피곤함 → 슬픔 → 필요한 휴식 → 등의 따스함 → 세 곡 추천
- 첫 곡 맛보기 → 곡 선택 → 본 감상
- 감상 마치기 → 포근한 이불의 심상 예시 → 이미지 일기 저장 → 저장한 일기 다시 열기

질문이 끝나는 시점은 실제 네이티브 `speaking` 상태로 기다립니다. 음악 구간에서는 시스템의 `AudioManager.isMusicActive`만 보지 않고 **앱 자체 MediaPlayer의 `musicPlaying === true`, `durationMs > 0`, `positionMs`가 250ms 넘게 증가하는 것**도 확인합니다. 이는 앱의 음악 재생 상태·진행을 검증하는 것이며, 실제 스피커에서 소리가 들렸다는 증거는 아닙니다.

대답은 앱에 준비된 예시 버튼으로 입력합니다. 사용자 마이크의 음성을 듣거나 인식 결과를 주입하는 테스트가 아닙니다. 심상 그림도 미리 제작한 포근한 이불 예시이며, 실행 중 이미지 생성 API를 호출하지 않습니다. TTS/STT 서비스의 초기화 성공과 실제 음성 입출력 성공을 구분합니다.

### 증거 파일

| 파일 | 확인할 내용 |
| --- | --- |
| `commit.txt`, `official-image-metadata.json`, `avd-config.ini` | 실행한 코드·공식 시스템 이미지·실제 AVD 설정 |
| `sys.boot_completed.txt` | OS 부팅 완료 여부 |
| `apk-install.log`, `app-launch.txt`, `activity.txt` | 앱 설치·Activity 실행 결과 |
| `app-screen.png`, `app-window.xml`, `pleos-native-startup-silent.mp4` | 실제 앱 시작 화면·접근성 구조·무음 시작 영상 |
| `capture-status.json` | 시작 화면·영상 캡처 파일 확보 여부. SDK 준비와 별도 판정 |
| `native-sdk-status.json`, `native-sdk-status-samples.json` | 실제 SDK 최종 상태와 준비 대기 중 관찰한 상태·오류 |
| `verification.json` | 부팅·SDK 준비 단계의 요약. 음성·마이크·실시간 음성 대화는 미검증으로 기록 |
| `journey-instrumentation.log`, `journey-verification.json` | 전체 UI 계측 실행 결과와 영상 확보 여부 |
| `journey-evidence/steps.json`, `journey-evidence/result.json` | 실제 버튼 조작·체크포인트·SDK 상태, 앱 자체 재생 위치 증가 검사, 저장·재열기 결과 |
| `journey-evidence/00-native-ready.png`부터 `08-diary-reopened.png` 및 같은 이름의 `.xml` | 각 체크포인트의 실제 화면과 접근성 구조. 실패하면 `failure.png`·`.xml`도 확인 |
| `pleos-native-journey-silent.mp4` | 최대 180초의 실제 UI 여정 무음 녹화. 영상 길이만으로 전체 여정 성공을 판단하지 않음 |

`verification.json`의 `fullConversationVerified: false`는 실시간 음성 대화를 검증하지 않았다는 뜻입니다. 버튼으로 진행한 전체 UI 여정의 통과 여부는 **`journey-verification.json`과 `journey-evidence/result.json`**에서 별도로 확인합니다. 서비스 준비나 MediaPlayer 진행 검사를 통과해도 실제 마이크 인식·스피커 출력·실차 동작은 미검증입니다.

준비 단계가 실패하면 UI 여정은 실행하지 않습니다. 계측 단계가 실패하거나 제한 시간에 도달하면 성공으로 바꾸지 않고, 확보된 부분 캡처·로그·결과를 남깁니다. 실패 시 `image-install.log`, `emulator.log`, `logcat.txt`, 서비스 상태 및 계측 로그를 살펴보세요.

10분 안에 부팅되지 않으면 중단합니다. UI 계측 명령은 165초, 여정 녹화는 최대 180초, 전체 작업은 최대 35분으로 제한합니다. 공개 저장소의 표준 Linux runner를 사용하며 유료 GPU나 AI API로 자동 전환하지 않습니다. 로그·JSON·화면 캡처·무음 영상은 7일간 artifact로 보관합니다. **시스템 이미지, SDK AAR 원본, 앱·계측 APK는 이 workflow의 공개 artifact에 포함하지 않습니다.** 자동 출시나 App Market 제출도 하지 않습니다.

## 공식 근거

- [Pleos ADB 설치·화면 녹화 안내](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide)
- [Android headless emulator 명령](https://developer.android.com/studio/run/emulator-commandline)
- [공개 저장소 GitHub 무료 Linux runner와 Android 가속](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)
- [Android custom repository 설정 구현](https://android.googlesource.com/platform/tools/base/+/mirror-goog-studio-main/repository/src/main/java/com/android/repository/impl/sources/LocalSourceProvider.java)
