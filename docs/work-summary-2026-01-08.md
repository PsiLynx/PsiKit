# Work Summary (2026-01-08)

This document summarizes the changes made across **PsiKit** and **TeraBridges/2025_Decode** during the recent “LinearOpMode + automatic logging” consolidation effort.

## Goals

- Fix a couple of logging correctness issues (profiling timestamps and console capture restore).
- Improve the FTC integration so LinearOpMode users can get automatic session management (via wrapper) and optional explicit per-loop ticking.
- Keep two minimal linear examples:
  - One using `LinearOpMode + FtcLoggingSession` (manual per-loop wiring)
  - One using `@PsiKitAutoLog` with explicit per-loop ticking helpers (recommended)

## PsiKit: Core fixes

- `core/src/main/java/org/psilynx/psikit/core/Logger.java`
  - Switched profiling timing inside `Logger.periodicAfterUser()` from `getTimestamp()` to `getRealTimestamp()`.
  - Rationale: `getTimestamp()` is effectively constant within a cycle, making durations meaningless.

- `core/src/main/java/org/psilynx/psikit/core/ConsoleSourceImpl.java`
  - Fixed `close()` restoring stderr incorrectly (was restoring stdout twice).

## PsiKit: FTC integration improvements

### LinearOpMode notes

- A `com.qualcomm...` SDK-package LinearOpMode replacement was explored for automatic per-event-loop ticking, but upstream prefers avoiding SDK-package extensions.
- Instead, use the AutoLog wrapper for automatic start/end, and add explicit per-loop ticking calls when you need per-loop timing/bulk-cache behavior.

Important behavior note:
- For automatic ticking, user code should prefer `opModeInInit()` / `opModeIsActive()` / `waitForStart()` loops.
- Loops written only as `while (!isStopRequested())` will not necessarily tick logging.

### FtcLoggingSession API: Java-friendly configure hook

- Updated `ftc/src/main/java/org/psilynx/psikit/ftc/FtcLoggingSession.kt`
  - `start(...)` supports an optional `configure` lambda executed after `Logger.reset()` / receiver setup but before `Logger.start()`.
  - Added `startWithConfigure(...)` overload so Java callers can use the *default filename* and still provide a configure hook.
    - Passing `null` for filename is not a valid way to “trigger the default” (Kotlin defaults are applied only when omitted).
    - Passing `""` disables file output.

### Iterative base timing

- Updated `ftc/src/main/java/org/psilynx/psikit/ftc/PsiKitIterativeOpMode.kt`
  - Populates `Logger.periodicAfterUser(userCodeLen, periodicBeforeLen)` with real timing deltas based on `Logger.getRealTimestamp()`.
  - Added/used an `onPsiKitConfigureLogging()` hook, plumbed into `FtcLoggingSession.start(configure = ...)`.

### Legacy compatibility

- Updated `ftc/src/main/java/org/psilynx/psikit/ftc/PsiKitOpMode.kt`
  - Kept as a deprecated compatibility layer; guidance text updated to prefer:
    - `PsiKitIterativeOpMode` (iterative)
    - For linear OpModes, use the AutoLog wrapper and (optionally) explicit per-loop ticking helpers.

### Removed unused linear bases

After confirming no internal references:
- Removed `ftc/src/main/java/org/psilynx/psikit/ftc/PsiKitLinearSessionOpMode.kt`
- Removed `ftc/src/main/java/org/psilynx/psikit/ftc/PsiKitThreadedLinearOpMode.kt`

## Docs updates

- Updated `docs/usage.md`
  - Now documents two supported linear patterns:
    1) Manual session (`LinearOpMode + FtcLoggingSession`)
    2) Automatic session (AutoLog wrapper)
  - Explicitly calls out the `opModeIsActive()` / `opModeInInit()` requirement for automatic ticking.

## 2025_Decode: minimal examples

In `TeraBridges/2025_Decode/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/opmodes/tests/`:

- `PsiKitLinearSessionMinimal.java`
  - Kept as the **manual** example: `LinearOpMode + FtcLoggingSession` with explicit per-loop calls.

- `PsiKitLinearOpModeMinimal.java`
  - Replace with the AutoLog examples + explicit per-loop ticking for linear-style code.

## Build/validation notes

- PsiKit FTC builds were run and succeeded:
  - `:ftc:compileDebugKotlin`
  - `:ftc:compileDebugJavaWithJavac`

- `2025_Decode :TeamCode:compileDebugJavaWithJavac` currently fails due to unrelated missing symbols (e.g., `AutoPoses` variables and `GlobalVariables.autoFollowerValid`).

## Git

- Changes were committed and pushed to both repos (PsiKit and 2025_Decode) on their current branches.
