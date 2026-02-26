# Usage Guide

## Quick Start (recommended): Iterative OpMode + `@PsiKitAutoLog`

This is the fastest way to get PsiKit working: annotate an iterative OpMode, then just log your own values with `Logger.recordOutput(...)`.

> PsiKit auto-logging can also record live data via RLOG. If you use the live server, it **must** be disabled during competition to be legal.

```java
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

@TeleOp(name = "Logged TeleOp")
@PsiKitAutoLog
public class LoggedTeleOp extends OpMode {
    @Override
    public void init() {
        Logger.recordMetadata("Robot", "MyRobot");
        Logger.recordMetadata("Mode", "TeleOp");
    }

    @Override
    public void loop() {
        Logger.recordOutput("Loop/RuntimeSec", getRuntime());
        Logger.recordOutput("Drive/Cmd/Forward", -gamepad1.left_stick_y);
        Logger.recordOutput("Drive/Cmd/Turn", gamepad1.right_stick_x);
    }
}
```

Want to change ports/folder/filename, opt in only, or configure LinearOpModes? See [FTC Auto-Logging](ftc-autolog-examples.md).

### FTC logging/tuning quick defaults

Current important defaults:

- AutoLog global enable: `false` (opt-in by `@PsiKitAutoLog`)
- `FtcLogTuning.bulkOnlyLogging`: `true`
- `FtcLogTuning.prefetchBulkDataEachLoop`: `true`
- `FtcLogTuning.processColorDistanceSensorsInBackground`: `true`
- `FtcLogTuning.logImu`: `false`

For the full knob list (autolog settings + all `FtcLogTuning` options), see
[FTC Logging & Tuning Reference](ftc-logging-tuning-reference.md).

---

## Logging your own data
The class you will interact the most with is `Logger`. It acts as a manager for all the I/O going on.

## Other Methods
In addition to the methods listed above, here are the most common ways you will interact with Psi Kit:

### `Logger.recordOutput(String key, T value)`

Where `T` is any primitive, `String`, `Enum`, `LoggedMechanism2D`, or a class that implements `WPISerializable` or `StructSerializable`, or a 1-2D array of those types. This is how you make data available to advantage scope.

Data structures like `Pose2d` implement `StructSerializable`, so you can automatically use them. This method must be called once per loop for the data to stay on AdvantageScope. The logger has a concept of tables and subtables, and `key` is split on `/` characters, and the pieces are used as subtables. for instance, if you log `a` to `Foo/Bar` and `b` to `Foo/Baz`, you will see:
```
Foo
├─ Bar   a
└─ Baz   b
```
It is recommended to log things in the same file in the same parent table, using the class name, for instance.

### `Logger.getTimestamp()`

Returns the **current log/replay timestamp** in seconds.

- In a normal run, this is updated once per loop by PsiKit (during `Logger.periodicBeforeUser()`), so it should be **stable within the same loop**.
- In replay, this returns the **replayed** timestamp from the log entry.

If you need a timestamp for logic that should behave the same in live runs and replay, prefer `Logger.getTimestamp()` (this is what makes replay deterministic).

If you want a “real time since start” clock for profiling/performance analysis (where determinism doesn’t matter), use `Logger.getRealTimestamp()` instead.

### Sampling semantics (important for performance modes)

Some FTC wrappers support a low-overhead mode where background sampling is reduced or disabled (for example when using `FtcLogTuning.nonBulkReadPeriodSec`, `FtcLogTuning.processColorDistanceSensorsInBackground = false`, or `FtcLogTuning.bulkOnlyLogging = true`).

PsiKit follows this convention:

- Measurement values are only written when freshly sampled.
- Per-loop sampled flags are written so logs show freshness explicitly.
- Stale values are not repeatedly re-logged as if they were fresh.

Examples of sampled flags:

- `color/sampled`, `distance/sampled`
- `sampledOrientation`, `sampledRates`
- `voltage/sampled`

### Bulk prefetch timing attribution

For FTC REV hubs, PsiKit uses manual bulk caching and (by default) prefetches once per loop
(`FtcLogTuning.prefetchBulkDataEachLoop = true`).

This gives cleaner timing attribution:

- Bulk transaction time is reported in:
    - `PsiKit/sessionTimes (us)/BulkPrefetchTotal`
    - `PsiKit/logTimes (us)/BulkPrefetch/<hub>`
- Per-device log times no longer "blame" the first bulk-backed device read in the loop.

If your loop intentionally performs no bulk-backed reads, you can disable prefetch:

- `FtcLogTuning.prefetchBulkDataEachLoop = false`

### Classes such as `Pose2d` and `LoggedMechanism2d`

Most classes referenced in the advantage scope docs are available in Psi Kit, ones that are part of WPI are in `psikit.wpi.*`.
___

**The [AdvantageScope Tab Reference](https://docs.advantagescope.org/category/tab-reference) is a very good resource; things that work the same in Psi Kit as in the AdvantageKit examples will not be covered by these docs.**

### Example OpMode
If you prefer a base class (no annotations / no OpMode wrapping), extend `PsiKitIterativeOpMode`.
This is also a good fit if you want to explicitly disable the live RLOG server for competition.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.Logger;

import org.psilynx.psikit.ftc.PsiKitIterativeOpMode;

@TeleOp(name="ConceptPsiKitLogger")
public class ConceptPsiKitLogger extends PsiKitIterativeOpMode {

    // IMPORTANT: disable live streaming (RLOG server) during competition.
    @Override
    protected int getRlogPort() {
        return 0;
    }

    @Override
    protected void onPsiKitConfigureLogging() {
        // Runs after Logger.reset() but before Logger.start().
        // Use for metadata and any extra receiver setup.
        Logger.recordMetadata("Robot", "MyRobot");
        Logger.recordMetadata("Mode", "TeleOp");
    }

    @Override
    protected void onPsiKitLoop() {
        Logger.recordOutput("Drive/Cmd/Forward", -gamepad1.left_stick_y);
        Logger.recordOutput("Drive/Cmd/Turn", gamepad1.right_stick_x);
    }
}
```

## Manual setup (advanced)

If you don’t want auto-logging, there are two supported patterns:

1. **Extend `PsiKitIterativeOpMode` (recommended)** — minimal boilerplate, explicit lifecycle hooks, and no OpMode wrapping.
2. **Manual session** — use `FtcLoggingSession` directly and call `Logger.periodicBeforeUser()` / `Logger.periodicAfterUser(...)` once per loop (see the LinearOpMode example below).

### If you want to, you can also use linear OpModes

There are two supported patterns:

1) **Manual session (most explicit / works with any loop style)**

```java
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.FtcLoggingSession;

public class MyOpMode extends LinearOpMode {
    private final FtcLoggingSession psiKit = new FtcLoggingSession();

    @Override
    public void runOpMode() {
        try {
            psiKit.start(this, 5800);

            while (opModeInInit()) {
                Logger.periodicBeforeUser();
                psiKit.logOncePerLoop(this);
                // init logic
                Logger.periodicAfterUser(0.0, 0.0);
                idle();
            }

            waitForStart();

            while (opModeIsActive()) {
                Logger.periodicBeforeUser();
                psiKit.logOncePerLoop(this);
                // loop logic
                Logger.periodicAfterUser(0.0, 0.0);
                idle();
            }
        } finally {
            psiKit.end();
        }
    }
}
```

2) **Automatic session (AutoLog wrapper)**

PsiKit can automatically start/end a session using an OpMode wrapper. See [FTC Auto-Logging](ftc-autolog-examples.md) for examples.

For `LinearOpMode`, you can optionally add explicit per-loop ticking via `PsiKitAutoLogger.linearPeriodicBeforeUser(...)` and `PsiKitAutoLogger.linearPeriodicAfterUser(...)` (see `ftc-autolog-examples.md`).

## Next, [Install Advantage Scope](installAscope.md)
