# FTC Logging & Tuning Reference

This page is the single-source reference for PsiKit FTC logging behavior knobs.

## Scope

Two places control behavior:

- **AutoLog configuration** (`PsiKitAutoLogSettings`, `@PsiKitAutoLog`) controls if/how sessions are auto-started.
- **Runtime tuning** (`FtcLogTuning`) controls per-loop logging cost/coverage.
- **Session behavior** (`FtcLoggingSession`) controls per-session startup details like device registry reset.

---

## FtcLoggingSession lifecycle knobs

Source: `FtcLoggingSession.kt`

- `FtcLoggingSession.defaultClearDeviceRegistryOnStart = true` (global default)
  - Applies to newly-created `FtcLoggingSession` instances.
  - Set this once at app/robot init to change the default for all future sessions.

- `clearDeviceRegistryOnStart = true` (default)
  - On `start(...)`, clears `HardwareMapWrapper.devicesToProcess` so stale wrapper entries from prior OpModes are removed.
  - This is still an **instance field** and can override the global default per session.

### Singleton/static subsystem pattern

If your robot code keeps wrapped hardware references in static/singleton subsystems and does not remap hardware each OpMode, set:

- `FtcLoggingSession.defaultClearDeviceRegistryOnStart = false` (set once globally), or
- `loggingSession.clearDeviceRegistryOnStart = false` (set per session)

Otherwise, the startup clear can remove entries and those devices will not be logged until a new `hardwareMap.get(...)` path re-registers wrappers.

Practical guidance:

- If each OpMode creates its own `FtcLoggingSession`, set the flag in each OpMode (or in a shared base class constructor/init path).
- If OpModes share one `FtcLoggingSession` instance, setting it once on that shared instance is enough.

---

## AutoLog defaults and options

Source: `PsiKitAutoLog.kt`

### Global defaults (`PsiKitAutoLogSettings`)

- `enabledByDefault = false`
  - AutoLog is opt-in by default unless enabled globally.
- `enableLinearByDefault = true`
  - Linear OpModes can auto-start/end sessions, but full per-loop wrapping still requires helper calls.
- `PROPERTY_RLOG_PORT` default: `5800`
- `PROPERTY_RLOG_FOLDER` default: `/sdcard/FIRST/PsiKit/`
- `PROPERTY_RLOG_FILENAME` default: `""` (auto-generated name)

### Per-OpMode annotation (`@PsiKitAutoLog`)

- `rlogPort: Int = 5800`
- `rlogFolder: String = "/sdcard/FIRST/PsiKit/"`
- `rlogFilename: String = ""`
- `writeRlogFile: Boolean = true`

### Opt-out annotation

- `@PsiKitNoAutoLog` forces opt-out when global enable is on.

---

## Runtime tuning defaults (`FtcLogTuning`)

Source: `FtcLogTuning.kt`

### Primary performance profile

- `bulkOnlyLogging = true`
  - Restricts logging to bulk-backed paths.
- `prefetchBulkDataEachLoop = true`
  - Issues one bulk read per hub at loop start for cleaner timing attribution.
- `nonBulkReadPeriodSec = 0.0`
  - No extra non-bulk rate-limiting by default.

### Sensor/background behavior

- `processColorDistanceSensorsInBackground = true`
  - Color/distance wrappers sample in background and cache values.
- `logImu = false`
  - IMU logging disabled by default.
- `logMotorCurrent = false`
- `motorCurrentReadPeriodSec = 0.1`

### Pinpoint behavior

- `pinpointReadPeriodSec = 0.0`
  - If `> 0`, limits Pinpoint sampling to that period (seconds); last pose is still written each loop.
- `pinpointLoggerCallsUpdate = true`
  - If `true`, PsiKit calls `pinpoint.update()` during logging; set `false` if your robot loop already updates Pinpoint to avoid duplicate I2C transactions.
- `pinpointUseMinimalBulkReadScope = false`
  - If `true` (and supported by firmware), uses a reduced Pinpoint read payload for lower I2C overhead.
- `pinpointWrapperPublishesOdometry = false`
  - If `true`, Pinpoint wrapper also publishes `/Odometry/<name>` entries; keep `false` to avoid duplicate odometry sources.
- `pedroFollowerPublishesNamedOdometry = false`
  - If `true`, Pedro follower logger also emits `/Odometry/<name>` and `/Odometry/<name>/PedroInches` alongside canonical odometry keys.

---

## Timing attribution notes

When `prefetchBulkDataEachLoop = true`:

- Bulk transaction cost appears in:
  - `PsiKit/sessionTimes (us)/BulkPrefetchTotal`
  - `PsiKit/logTimes (us)/BulkPrefetch/<hub>`
- This avoids over-attributing the first bulk-backed device read each loop.

`HardwareMapByType/*` timing buckets cover wrapper processing inside `FtcLoggingSession.logOncePerLoop()`.

If user code explicitly calls hardware getters (for example `getNormalizedColors()` inside OpMode loop), that cost contributes to user-code timing (such as `LoggedRobot/UserCodeMS`) unless separately instrumented.

---

## Recommended presets

### Low-overhead default-style profile

- Keep defaults as-is (`bulkOnlyLogging=true`, prefetch on, IMU off).
- Use for tight loop-rate operation and broad hardware logging with low overhead.

### Deep diagnostics profile

- `bulkOnlyLogging = false`
- `logImu = true`
- Optional: reduce `nonBulkReadPeriodSec` and enable specific expensive signals temporarily.
- Use for short targeted debugging sessions, not for sustained high-rate operation.
