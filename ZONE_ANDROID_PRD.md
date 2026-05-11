# ZONE Android MVP PRD

## 1. 제품 정의
ZONE은 사용자가 직접 가져온 영상을 재생하는 동안, **폰의 고정 상태와 사용자의 정면 주시 상태가 유지될 때만 재생이 계속되는 강제 몰입 플레이어**다.

이 PRD는 **일반 소비자 안드로이드 폰(BYOD)에서 테스트 가능한 MVP**를 기준으로 작성한다. 따라서 “앱 내부에서는 강하게 통제”하되, “기기 전체를 완전 잠금”하는 수준은 범위 밖으로 둔다.

---

## 2. 목표
- 안드로이드 앱 하나로 ZONE의 핵심 가치 검증
- 사용자가 직접 첨부한 로컬 영상 재생
- 영상 시청 중 이동/기울임/시선 이탈 시 자동 일시정지
- 세션 종료 후 간단한 집중 리포트 제공
- 사용자 반응 검증: “불편하지만 계속 쓰고 싶다” 수준의 강제성 찾기

## 3. 비목표
- 일반 소비자 폰에서 시스템 전체를 완전 잠그는 것
- 백그라운드에서 카메라로 계속 사용자를 감시하는 것
- 타 앱을 강제로 종료/삭제/차단하는 것
- 서버 기반 추천/소셜 기능
- iOS 동시 지원

---

## 4. 핵심 가설
1. 사용자는 “몰입용 영상 플레이어”라는 명확한 맥락에서는 강한 제재를 받아들인다.
2. 시선 + 자세 + 움직임 결합만으로도 체감상 충분히 강한 몰입 환경을 만들 수 있다.
3. 완전한 기기 잠금이 없어도, 영상 재생 자체를 멈추는 방식만으로 MVP 검증이 가능하다.

---

## 5. 타깃 사용자
### 1차
- 공부/강의/독서용 영상을 의도적으로 보는 사용자
- 유튜브/강의 녹화/강연/명상 영상 등을 “끝까지 집중해서” 보고 싶은 사용자
- 자기통제보다 환경통제를 선호하는 사용자

### 2차
- 고시/수험/토익/자격증/개발 강의 시청자
- ADHD 성향 또는 주의 분산이 심한 사용자
- 디지털 디톡스/루틴 앱에 관심 있는 사용자

---

## 6. MVP 범위
### 포함
- 로컬 영상 첨부 및 재생
- 세션 생성
- 전면 카메라 기반 얼굴 존재/정면 여부 판단
- 센서 기반 고정 상태 판단
- 위반 시 경고/일시정지/재정렬
- 세션 종료 리포트
- 선택적 DND 유도
- 선택적 Screen Pinning 진입 유도

### 제외
- 정밀 안구 추적
- 시스템 알림 완전 제거
- 일반 사용자 폰에서 홈/최근앱 완전 봉쇄
- 기업용 Device Owner/MDM 모드
- 클라우드 동기화

---

## 7. 사용자 시나리오
### 시나리오 A: 첫 사용
1. 사용자가 앱 설치 후 실행
2. 로컬 영상 첨부
3. 카메라 권한 허용
4. 몰입 강도 선택 (라이트 / 스탠다드 / 하드)
5. 폰을 거치대에 두고 3초간 정렬
6. 세션 시작
7. 영상 재생 중 이동/기울임/시선 이탈 시 즉시 경고 후 일시정지
8. 안정 상태 회복 시 재생 재개
9. 종료 후 리포트 확인

### 시나리오 B: 재사용
1. 최근 첨부 영상 재선택
2. 이전 세션 기준으로 자동 보정된 임계값 적용
3. 바로 세션 시작

---

## 8. 핵심 UX 원칙
1. **통제는 강하게, 설명은 명확하게**
2. **벌점보다 재정렬 경험이 중요**
3. **사용자가 억울하다고 느끼면 바로 이탈**
4. **정밀함보다 일관성**

---

## 9. 기능 요구사항

### FR-1. 로컬 영상 첨부
- 사용자는 기기 내 비디오 파일을 선택할 수 있어야 한다.
- 앱은 최소 1개의 비디오를 라이브러리에 저장해 재생 목록처럼 보여줄 수 있어야 한다.
- 저장 항목:
  - id
  - contentUri
  - 제목
  - 길이
  - 썸네일
  - 추가 시각

### FR-2. 세션 생성
- 사용자는 재생할 영상을 고른다.
- 사용자는 몰입 강도를 선택한다.
- 앱은 세션 시작 전 “정렬 화면”으로 이동한다.

### FR-3. 정렬/캘리브레이션
- 3초 동안 아래 조건을 만족해야 시작 가능:
  - 얼굴 검출됨
  - 얼굴이 화면 중앙 근처
  - 폰 움직임이 작음
  - 기울기 변화가 작음
- 캘리브레이션 결과 저장:
  - 기준 회전값
  - 기준 얼굴 크기/위치
  - 기준 머리 자세값

### FR-4. 재생 제어
- 조건 만족 시 재생
- 위반 시 즉시 일시정지
- 회복 시 재생 재개
- 사용자는 세션 중 탐색/배속/앱 전환을 제한적으로만 수행 가능

### FR-5. 위반 감지
#### 이동 위반
- 선형 가속도 급증
- 자이로 변화량 급증
- 기준 회전값 대비 각도 편차 증가

#### 시선/자세 위반
- 얼굴 미검출
- 고개 yaw/pitch/roll 편차 증가
- 얼굴이 화면 밖 또는 너무 작아짐

### FR-6. 단계별 제재
- 1단계: 배너 경고 + 짧은 진동/소리
- 2단계: 즉시 pause + 어두운 오버레이
- 3단계: 재정렬 2.5초 요구
- 4단계: 짧은 lockout (예: 10초)
- 5단계: 세션 재캘리브레이션 요구

### FR-7. 세션 리포트
- 총 세션 시간
- 실제 재생 시간
- 움직임 위반 횟수
- 시선 이탈 횟수
- 평균 안정 구간 길이
- 완료율
- 집중 점수 (0~100)

---

## 10. 판별 로직 초안

### 입력
- Rotation Vector
- Gyroscope
- Accelerometer / Linear Acceleration
- (선택) Significant Motion
- Front Camera + Face Detection

### 주요 파생 지표
#### 1) postureDeviation
- 기준 회전값과 현재 회전값의 각도 차이
- 의미: 폰이 얼마나 기울어졌는지

#### 2) motionEnergy
- 최근 1초 윈도우에서 선형가속도 RMS + 자이로 RMS
- 의미: 폰을 들었는지, 흔들었는지

#### 3) faceConfidence
- 얼굴 존재 여부
- 얼굴 bbox 크기
- 중앙 정렬 정도

#### 4) attentionProxy
- 얼굴 존재 + 정면 머리 자세 + 일정 시간 유지
- 정밀 eye gaze 대신 “정면 주시 근사치” 사용

### 상태 머신
- CALIBRATING
- READY
- PLAYING
- WARNING
- PAUSED_MOVEMENT
- PAUSED_ATTENTION
- RECOVERING
- SESSION_DONE

### 초기 임계값(실험용)
- postureDeviation > 12도 for 700ms → 경고
- postureDeviation > 20도 for 500ms → pause
- motionEnergy > threshold(기기별 정규화) for 500~800ms → pause
- face missing > 1000ms → pause
- head yaw/pitch 절대값 > 18~22도 for 800ms → pause
- recover stable 2500ms → resume

### 적응형 보정
- 첫 3세션은 기본 임계값
- 이후 안정 구간의 분포를 기반으로 사용자별 threshold 조정
- 오탐이 많은 사용자는 threshold를 10~15% 완화

---

## 11. 기술 아키텍처

### 앱 스택
- Kotlin
- Jetpack Compose
- MVVM
- Media3 ExoPlayer
- CameraX ImageAnalysis
- ML Kit Face Detection (MVP)
- SensorManager
- Room
- DataStore
- Coroutines + Flow

### 추천 모듈 구조
- app
- core/model
- core/data
- core/ui
- feature/library
- feature/session
- feature/player
- feature/sensors
- feature/focuscamera
- feature/report

### 데이터 저장
#### Room
- video_items
- sessions
- session_events
- calibration_profiles

#### DataStore
- 앱 설정
- strictness level
- permission onboarding 상태
- 최근 사용 영상

### 처리 위치
- MVP는 전부 온디바이스
- 서버 없음
- 카메라 프레임 저장 없음
- 센서 원시 로그는 샘플링 저장 또는 익명 집계만

---

## 12. 플랫폼 제약을 반영한 제품 결정

### 일반 소비자 앱에서 가능한 것
- 앱 안에서 영상 재생 제어
- 센서 기반 이동/기울기 감지
- 전면 카메라 기반 얼굴 존재/정면 여부 판단
- 세션 중 DND 유도
- 사용자가 동의한 범위의 Screen Pinning

### 일반 소비자 앱에서 불가능하거나 비현실적인 것
- 홈/최근앱/상단바/알림을 완전 봉쇄
- 백그라운드에서 카메라로 계속 시선 추적
- 타 앱을 강제로 종료/봉쇄
- 물리적으로 폰 위치 조정을 “못 하게” 만드는 것
- 정밀 eye-tracking 하드웨어 수준의 시선 추적

### 어려운 것
- 다양한 OEM 기기에서 센서 노이즈 보정 일관성 확보
- 저조도/안경/마스크/역광 환경에서 안정적인 얼굴 판별
- 오탐 없이 충분히 빡센 UX 만들기
- Play 심사를 통과하면서 강한 통제를 유지하기

---

## 13. MVP 정책 전략
- **Play 스토어 배포를 고려한 안전한 MVP**를 우선한다.
- AccessibilityService를 이용해 타 앱을 조작하는 전략은 MVP에서 제외한다.
- Device Owner / Kiosk / MDM 모드는 별도 실험 브랜치로 분리한다.

---

## 14. 성공 지표
### 제품 지표
- 1일차 세션 시작률
- 1주차 재사용률
- 평균 세션 완료율
- 세션당 평균 실제 재생 시간
- 사용자당 주간 세션 수

### 품질 지표
- false pause 비율
- 카메라 인식 실패율
- 재캘리브레이션 요청 비율
- 크래시율
- 배터리 소모율

### 반응 지표
- “생각보다 강해서 좋다” 비율
- “너무 빡세다” 비율
- “억울한 정지” 비율

---

## 15. 출시 단계

### Phase 1 — 2주
- 프로젝트 세팅
- Media3 로컬 영상 첨부/재생
- Room 저장
- 기본 세션 화면

### Phase 2 — 2주
- SensorManager 연동
- postureDeviation / motionEnergy 계산
- pause/resume 상태머신

### Phase 3 — 2주
- CameraX + ML Kit 얼굴 검출
- face present / head pose 판별
- 센서+카메라 결합 규칙

### Phase 4 — 1~2주
- 리포트 화면
- 설정/강도 조절
- DND / Screen Pinning 온보딩

### Phase 5 — 2주
- 내부 테스트
- OEM 디바이스별 튜닝
- false positive 감소
- 베타 출시

---

## 16. Codex/AI 코딩 운영 방식
- PRD를 루트에 두고, AGENTS.md에 금지사항/코딩 규칙/모듈 구조를 고정한다.
- 기능을 다음 단위로 쪼개서 병렬 생성:
  1. player
  2. sensor engine
  3. camera analyzer
  4. report
  5. instrumentation tests
- 모든 알고리즘은 먼저 pure Kotlin로 함수화하고 단위 테스트를 만든 뒤 UI에 연결한다.
- 센서 로그를 CSV 재생 방식으로 테스트할 수 있게 해, 실제 디바이스 없이도 회귀 검증이 가능하게 한다.

---

## 17. 네가 익혀야 할 기술
### 반드시
- Kotlin + Compose
- Android lifecycle / permission model
- Media3 ExoPlayer
- CameraX ImageAnalysis
- ML Kit Face Detection
- SensorManager / rotation vector / quaternion 기초
- Room / DataStore
- Android 백그라운드/FGS 제약
- Google Play 정책(특히 Accessibility / 민감 권한)

### 나중에
- ML Kit Face Mesh
- Play Integrity API
- Device Owner / Dedicated Device / Test DPC
- Baseline Profiles / Macrobenchmark
- Firebase Remote Config 또는 자체 실험 플래그

---

## 18. 최종 제품 결정
### 추천 MVP 포지셔닝
**“강제 집중 영상 플레이어”**

### 피해야 할 포지셔닝
- “기기 전체를 완전히 잠그는 앱”
- “타 앱을 막아주는 관리자 앱”

### 이유
영상 플레이어라는 명확한 컨텍스트 안에서 통제하면 사용자가 납득한다. 반면 기기 전체 통제는 기술적/정책적/심리적 비용이 너무 크다.

---

## 19. 다음 문서로 바로 이어질 것
1. Technical Design Doc
2. Sensor Threshold Spec
3. Camera Focus Spec
4. Room Schema
5. AGENTS.md
6. Codex task backlog
