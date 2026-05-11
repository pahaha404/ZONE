# AGENTS.md — ZONE Android

## Product goal
Build an Android MVP for ZONE: a forced-focus video player that only keeps playing when the phone stays stable and the user remains facing the screen.

## Non-goals
- Do not implement system-wide device lockdown for BYOD.
- Do not use AccessibilityService to control other apps.
- Do not add backend dependencies unless explicitly requested.
- Do not upload camera frames.

## Stack
- Kotlin
- Jetpack Compose
- MVVM
- Media3 ExoPlayer
- CameraX + ML Kit Face Detection
- SensorManager
- Room + DataStore
- Coroutines + Flow

## Architecture rules
- Keep algorithms testable as pure Kotlin where possible.
- UI code must not contain sensor math.
- Put threshold logic behind interfaces.
- Prefer small feature modules.
- Every non-trivial feature must include tests.

## Implementation order
1. Local video import + playback
2. Session state machine
3. Sensor engine
4. Camera analyzer
5. Report screen
6. Tuning + test harness

## Safety rules
- Never claim true eye tracking unless implemented and validated.
- Treat current attention detection as a proxy using face presence + head pose.
- Keep all processing on-device.
- Avoid restricted or policy-risky APIs unless explicitly approved.

## Coding rules
- Use clear names.
- Add KDoc to public classes.
- Favor immutable state.
- Prefer StateFlow for screen state.
- Do not silently swallow exceptions.

## Testing rules
- Add unit tests for threshold/state machine logic.
- Add fake sensor streams for replay testing.
- Add instrumentation tests for player and permission flows.
