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

## 결과 해석

이 workflow는 **네이티브 앱 시작 화면과 SDK 초기화**를 확인하는 실험이며, 앱의 전체 대화·음악 감상 여정을 녹화하는 시연은 아닙니다.

1. `sys.boot_completed.txt`가 `1`이어야 실제 부팅 완료입니다. `adb devices`에 나타나는 것만으로 성공하지 않습니다.
2. `app-launch.txt`, `activity.txt`, `app-screen.png`, `pleos-native-startup-silent.mp4`로 APK 실행과 실제 화면을 확인합니다. 검은 화면·OS 차단 화면은 사람이 검토해야 합니다.
3. 디버그 WebView가 제공하는 실제 `PleosNative.getStatus()`를 읽어 `native-sdk-status.json`에 저장합니다. `ttsReady`, `sttReady`, `vehicleConnected`가 모두 `true`이고 AAOS UX 제한 상태가 실제 boolean이어야 SDK 준비 확인 단계가 통과합니다. 실패 값을 수정하거나 모의 값으로 바꾸지 않습니다.
4. WebView 디버깅 소켓이 없거나 서비스가 준비되지 않으면 해당 단계는 실패합니다. APK 실행 증거와 부분 상태는 그대로 남깁니다. UI를 띄운 사실을 음성 서비스 실행 성공으로 표현하지 않습니다.
5. `verification.json`에는 실제 음성·마이크·전체 대화 미검증과 별도 시각 검토 필요를 항상 기록합니다.

실패 시 `image-install.log`, `emulator.log`, `logcat.txt` 및 서비스 상태를 살펴보세요. 10분 안에 부팅되지 않으면 중단하며, 전체 작업은 최대 35분으로 제한합니다. 로그와 캡처만 7일간 artifact로 보관합니다. **시스템 이미지, SDK 바이너리, APK는 이 workflow의 공개 artifact에 포함하지 않습니다.** 자동 출시나 App Market 제출도 하지 않습니다.

## 공식 근거

- [Pleos ADB 설치·화면 녹화 안내](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide)
- [Android headless emulator 명령](https://developer.android.com/studio/run/emulator-commandline)
- [공개 저장소 GitHub 무료 Linux runner와 Android 가속](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)
- [Android custom repository 설정 구현](https://android.googlesource.com/platform/tools/base/+/mirror-goog-studio-main/repository/src/main/java/com/android/repository/impl/sources/LocalSourceProvider.java)
