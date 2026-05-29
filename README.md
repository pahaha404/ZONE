# ZONE

ZONE은 사용자가 가져온 로컬 영상을 재생하는 동안 폰의 안정 상태와 사용자의 정면 주시 상태를 확인해, 조건이 유지될 때만 재생을 계속하는 Android 몰입 세션 플레이어입니다.

이 프로젝트의 목표는 일반 소비자 Android 폰에서 앱 내부 재생 흐름을 강하게 통제하는 MVP를 검증하는 것입니다. 기기 전체 잠금, 다른 앱 제어, 백그라운드 감시, 서버 기반 분석은 범위에 포함하지 않습니다.

## 핵심 기능

- 로컬 영상 파일 가져오기 및 라이브러리 관리
- 영상별 몰입 세션 생성
- 세션 시작 전 3초 정렬 및 캘리브레이션
- SensorManager 기반 기기 움직임과 기울기 감지
- CameraX와 ML Kit Face Detection 기반 얼굴 존재 및 머리 자세 확인
- 위반 상태 감지 시 경고, 일시정지, 재정렬, 재캘리브레이션 흐름 처리
- 세션 종료 후 집중 시간, 이탈 횟수, 움직임 횟수, 집중 점수 리포트 제공

## 사용자 흐름

1. 앱에서 로컬 영상을 선택합니다.
2. 몰입 강도를 선택하고 세션을 준비합니다.
3. 전면 카메라와 센서 기준으로 정렬 상태를 캘리브레이션합니다.
4. 영상 재생 중 움직임, 기울기, 얼굴 이탈, 정면 주시 이탈을 감지합니다.
5. 안정 상태가 깨지면 재생을 멈추고, 안정 상태가 회복되면 재생을 재개합니다.
6. 세션이 끝나면 집중 리포트를 확인합니다.

## 기술 스택

- Kotlin 2.2.20
- Android Gradle Plugin 8.11.1
- Jetpack Compose
- MVVM
- Navigation Compose
- Media3 ExoPlayer
- CameraX ImageAnalysis
- ML Kit Face Detection
- Android SensorManager
- Room
- DataStore
- Coroutines, Flow
- JUnit4, AndroidX Test, Espresso, Truth

## 프로젝트 구조

| 경로 | 역할 |
| --- | --- |
| `app` | Android 앱 진입점, 의존성 조립, 화면 내비게이션 |
| `core:model` | 공통 도메인 모델 |
| `core:common` | 집중 점수, 센서 수치 계산, 쿼터니언 계산 등 순수 Kotlin 로직 |
| `core:database` | Room 데이터베이스, DAO, 엔티티, 저장소 구현 |
| `core:datastore` | 앱 설정과 사용자 선호값 저장 |
| `core:ui` | 공통 Compose UI 컴포넌트 |
| `feature:library` | 로컬 영상 가져오기와 영상 목록 |
| `feature:session` | 캘리브레이션, 세션 상태 머신, 세션 화면 |
| `feature:player` | Media3 기반 플레이어 제어 |
| `feature:sensors` | 움직임 및 자세 감지 엔진 |
| `feature:focuscamera` | 전면 카메라 기반 얼굴/주의 상태 분석 |
| `feature:report` | 세션 결과 리포트 |

## 요구 환경

- Android Studio
- JDK 17 이상
- Android SDK 36
- Android 8.0, API 26 이상 대상 기기 또는 에뮬레이터

앱은 카메라와 진동 권한을 사용합니다. 전면 카메라는 필수 하드웨어로 강제하지 않지만, 집중 감지 기능을 확인하려면 전면 카메라가 있는 기기가 필요합니다.

## 실행 방법

Windows:

```powershell
.\gradlew.bat assembleDebug
```

macOS 또는 Linux:

```bash
./gradlew assembleDebug
```

Android Studio에서는 프로젝트를 연 뒤 Gradle Sync를 완료하고 `app` 구성을 실행하면 됩니다.

## 테스트

단위 테스트:

```powershell
.\gradlew.bat test
```

연결된 기기 또는 에뮬레이터에서 계측 테스트:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

센서 임계값, 상태 머신, 리포트 계산처럼 사용자 경험에 직접 영향을 주는 로직은 가능한 한 순수 Kotlin으로 분리해 테스트합니다.

## 설계 원칙

- UI 코드에 센서 수학을 넣지 않습니다.
- 임계값과 판별 로직은 인터페이스 뒤에 둡니다.
- 알고리즘은 가능한 한 순수 Kotlin으로 유지합니다.
- 화면 상태는 StateFlow 기반으로 관리합니다.
- 카메라 프레임은 외부로 업로드하지 않고 기기 내에서 처리합니다.
- 현재 주의 감지는 정밀한 eye tracking이 아니라 얼굴 존재와 머리 자세를 이용한 정면 주시 근사치입니다.

## MVP 범위

포함:

- 로컬 영상 재생
- 센서 기반 움직임과 자세 감지
- 전면 카메라 기반 얼굴 존재 및 정면 여부 판단
- 위반 상태에 따른 경고와 재생 제어
- 세션 기록과 집중 리포트
- 선택적 DND, Screen Pinning 유도

제외:

- 시스템 전체 기기 잠금
- 다른 앱 강제 종료, 차단, 삭제
- AccessibilityService 기반 타 앱 제어
- 서버 기반 추천 또는 소셜 기능
- 카메라 프레임 업로드
- 정밀 안구 추적

## 참고 문서

- `ZONE_ANDROID_PRD.md`: Android MVP 제품 요구사항
- `ZONE_PRD_v1.md`: 실행 스펙
- `AGENTS.md`: 개발 지침과 안전 규칙
