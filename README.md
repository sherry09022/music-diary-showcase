# 음악일기 · Music Diary

**현대자동차 차량용 앱 개발 공모전 · 참가자 한수진**

음악일기는 음악을 감상하며 떠오르는 생각과 이미지를 일기로 기록하는 앱입니다. 현재의 감정과 몸의 감각을 살펴보고, 나에게 필요한 느낌에 어울리는 음악을 듣습니다. 음악과 함께 자신을 이해하고 돌보는 시간을 한 장의 일기로 남깁니다.

- [제출용 영상 바로 보기](https://sherry09022.github.io/music-diary-showcase/?v=live-exit-20261115)
- [60초 릴스 MP4 내려받기](https://sherry09022.github.io/music-diary-showcase/assets/music-diary-reel.mp4?v=instagram-vertical-20261115)
- [장면 구성 1](assets/reel-contact-sheet.jpg) · [장면 구성 2](assets/reel-contact-sheet-2.jpg)

## Pleos Connect 별도 개발 데모

- [녹화한 웹 시연 영상](https://sherry09022.github.io/music-diary-showcase/pleos-demo/watch.html)
- [웹 시연 인스타용 세로 편집본 · 1080×1920](https://sherry09022.github.io/music-diary-showcase/pleos-demo/video/music-diary-web-instagram-vertical.mp4)
- [차량용 웹 데모 체험](https://sherry09022.github.io/music-diary-showcase/pleos-demo/)
- [별도 소스·SDK 구성](pleos-demo/) · [Android 빌드 검증](https://github.com/sherry09022/music-diary-showcase/actions/workflows/pleos-demo.yml)
- [Pleos 차량 환경용 데모 APK](https://sherry09022.github.io/music-diary-showcase/pleos-demo/downloads/music-diary-pleos-debug.apk)

`pleos-demo/`는 기존 앱과 릴스에 영향을 주지 않는 별도 프로젝트입니다. 웹은 차량값을 모의로 사용하는 실행 가능한 체험판이며, Android 프로젝트는 실제 Pleos STT·TTS·Vehicle SDK를 사용합니다. 실제 차량/플레오스 에뮬레이터 실행 여부는 해당 폴더의 검증 안내를 참고하세요.

## 릴스의 이야기

비 오는 밤, 빗물 너머로 도시의 불빛이 번지는 실제 영상으로 시작합니다. 퇴근길 운전자는 전방을 보며 앱과 대화하고, 편안함을 느끼기 위해 Scott Buckley의 **Reverie**를 선택합니다. 선택 순간 배경음은 잦아들고 선택한 곡이 시작됩니다. 곡이 커지면서 야간 도로를 달리는 8초의 감상 장면이 이어집니다. 그 뒤 ‘음악을 듣고 떠오르는 이미지가 있을까요?’라는 질문을 받습니다. ‘음악을 듣고 포근한 이불의 느낌이 생각났어.’라는 답변에 맞춰 따뜻한 이불의 심상을 그림으로 보여주고, 음악과 자신의 말을 담은 일기로 저장합니다. 마지막에는 차 문을 열고 밖으로 나와 문을 닫는 실제 하차 영상이 이어집니다.

마지막 화면에는 **현대자동차 차량용 앱 개발 공모전 / 참가자 한수진**을 표시합니다.

## 영상 구성

| 시간 | 장면 |
| --- | --- |
| 0–3초 | 비 오는 밤의 실사 장면과 ‘퇴근했는데, 마음은 아직 야근 중.’ |
| 3–15초 | 피곤함·슬픔을 나누고 필요한 휴식 확인 |
| 15–20초 | 몸에서 등의 따스한 감각 발견 |
| 20–27초 | 따스함을 더 경험할 세 곡 추천·첫 곡 선택 |
| 27–30초 | 전방과 음악에 대한 짧은 안내, 선택곡 재생 시작·음량 증가 |
| 30–38초 | 실제 야간 주행을 보며 8초간 음악 감상, 재생 시간·음악에 반응하는 막대 표시 |
| 38–41초 | ‘음악을 듣고 떠오르는 이미지가 있을까요?’ |
| 41–46초 | ‘음악을 듣고 포근한 이불의 느낌이 생각났어.’ |
| 46–51초 | 새로 그린 포근한 이불의 심상·음악·글을 일기로 저장하는 시연 |
| 51–56초 | 차 문을 열고 밖으로 나와 문을 닫는 실제 하차 동작 |
| 56–60초 | 공모전·참가자 엔드카드 |

이 자료는 **앱과 별도로 제작한 콘셉트 릴스**입니다. 도입·주행·하차는 실사 영상입니다. 주행 구간은 와이퍼가 유리를 닦고 도시의 불빛과 도로 풍경이 움직이며, 대화를 앞유리에 비친 글씨처럼 합성했습니다. AI 이미지는 일기 시연에 담긴 포근한 이불의 심상 그림에 사용했습니다. 차창 대화·음악 재생 표시·일기 화면은 시연용 그래픽으로, 실제 앱 기능을 촬영한 영상은 아닙니다. 대사는 자막으로 표시하며 별도 음성 나레이션은 없습니다.

## 재생과 파일

일반 브라우저에서 위 영상 링크를 열어 재생하면 됩니다. 영상은 **1080×1920, 9:16, 30fps, 60초, H.264/AAC** 형식입니다. 이 저장소에는 동영상과 정적 소개 페이지를 담았습니다. 설치·로그인·API 키가 필요하지 않습니다.

로컬에서 보려면 이 폴더에서 `python3 -m http.server 8080`을 실행하고 `http://localhost:8080`을 엽니다.

```text
index.html                  영상 소개 페이지
styles.css                  페이지 스타일
assets/music-diary-reel.mp4  음악 포함 릴스
assets/reel-contact-sheet.jpg  장면 구성
assets/imagery-window.png   포근한 이불의 심상 그림
REFERENCES.md              수상작 참고와 음원 출처
POST_CAPTION.txt            SNS 게시용 설명·해시태그
```

운영 앱 소스, 서버 설정, API 키, 인증서, 실제 사용자 기록, 신청서·동의서 등 개인정보 문서는 포함하지 않습니다.

## 크레딧

- 기획: 한수진
- 포근한 이불의 심상 그림: 내장 AI 이미지 생성 도구로 제작
- 실사 도입 영상: **Teju / Pexels**, [Rainy Night Street with Blurred Lights](https://www.pexels.com/video/rainy-night-street-with-blurred-lights-35180823/) · [Pexels License](https://www.pexels.com/license/)
- 실사 주행 영상: **Ömer Safak / Pexels**, [Nighttime Rainy Highway Traffic Scene](https://www.pexels.com/video/nighttime-rainy-highway-traffic-scene-35584072/) · [Pexels License](https://www.pexels.com/license/)
- 실사 하차 영상: **Ron Lach / Pexels**, [A Woman Getting Out of a Car](https://www.pexels.com/video/a-woman-getting-out-of-a-car-9520614/) · [Pexels License](https://www.pexels.com/license/)
- 주행 영상에 색보정·슬로모션·차창 대화 그래픽을 적용했습니다. 하차 영상은 원본 9.2–18.8초를 발췌해 5초로 편집하고, 크롭·색보정을 적용했습니다. 실사 영상의 차량 브랜드와 장면 사이 동일 인물 여부는 확인하지 않았습니다.
- 음악: **‘Reverie’ by Scott Buckley — released under CC-BY 4.0. www.scottbuckley.com.au**
- [공식 음원과 이용 조건](https://www.scottbuckley.com.au/library/reverie/) · [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- 공식 음원의 50–83초 구간을 발췌해 영상 27초부터 재생하며 음량·페이드를 편집했습니다. 앞부분의 배경음, 빗소리와 도입의 낮은 펄스는 직접 합성했습니다. 배경음은 영상 25–27초에 사라지고 선택곡은 27–30초에 커집니다.

원곡 MP3 전체는 포함하지 않습니다. 영상 재게시 시에도 음악 크레딧을 유지해 주세요. 음악 등의 권리는 해당 권리자에게 있습니다.

수상작 공식 소개를 참고한 구성과 출처는 [REFERENCES.md](REFERENCES.md)에 정리했습니다. SNS 게시 시 사용할 설명과 공모전 해시태그는 [POST_CAPTION.txt](POST_CAPTION.txt)에 있습니다. 이 저장소 공개는 SNS 게시나 공모전 접수 완료를 의미하지 않습니다.
