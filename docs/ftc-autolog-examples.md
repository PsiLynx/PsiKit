# PsiKit FTC Auto-Logging Examples

PsiKit can automatically install logging around your OpModes without requiring you to:
- extend a PsiKit base class, or
- call `super` in lifecycle methods.

For all runtime tuning knobs and defaults (`FtcLogTuning` + AutoLog settings), see
[FTC Logging & Tuning Reference](ftc-logging-tuning-reference.md).

This works by registering an FTC event-loop hook (`@OnCreateEventLoop`), then (for iterative OpModes) swapping the active OpMode to a wrapper that runs PsiKit logging hooks around your callbacks.

## Imports

You’ll typically use these:

- `org.psilynx.psikit.ftc.autolog.PsiKitAutoLog`
- `org.psilynx.psikit.ftc.autolog.PsiKitNoAutoLog`
- `org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings`

---

## 1) Enable global logging, opt out specific OpModes

### Global enable (explicit)

By default, PsiKit’s auto-logger is **disabled**.

If you want global behavior (instrument iterative OpModes automatically), enable it at runtime.

#### Kotlin (recommended: configure at event-loop creation)
<!-- tabs:start -->

#### **Kotlin**

```kotlin
package org.firstinspires.ftc.teamcode

import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings

object PsiKitConfig {
    @JvmStatic
    @OnCreateEventLoop
    fun configure(context: Context, ftcEventLoop: FtcEventLoop) {
        PsiKitAutoLogSettings.enabledByDefault = true
        PsiKitAutoLogSettings.enableLinearByDefault = true
    }
}
```

#### **Java**

```java
package org.firstinspires.ftc.teamcode;

import android.content.Context;

import com.qualcomm.ftccommon.FtcEventLoop;

import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings;

public final class PsiKitConfig {
    @OnCreateEventLoop
    public static void configure(Context context, FtcEventLoop ftcEventLoop) {
        PsiKitAutoLogSettings.enabledByDefault = true;
        PsiKitAutoLogSettings.enableLinearByDefault = true;
    }
}
```

<!-- tabs:end -->

### Set the RLOG port globally

If global auto-logging is enabled, the default port comes from:

- `System.getProperty("psikit.autolog.rlogPort")` (preferred for configuration), else
- the built-in default (5800).

On Android/FTC, the simplest is to set the property in an `@OnCreateEventLoop` hook:

```kotlin
import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings

object PsiKitConfig {
    @JvmStatic
    @OnCreateEventLoop
    fun configure(context: Context, ftcEventLoop: FtcEventLoop) {
        // Global default for all auto-logged OpModes
        System.setProperty(PsiKitAutoLogSettings.PROPERTY_RLOG_PORT, "5900")
        System.setProperty(PsiKitAutoLogSettings.PROPERTY_RLOG_FOLDER, "/sdcard/FIRST/PsiKit/")
        // Optional:
        // System.setProperty(PsiKitAutoLogSettings.PROPERTY_RLOG_FILENAME, "my-run.rlog")
    }
}
```

### Example: logging your own outputs (iterative)

Once PsiKit is auto-installed (global enable or `@PsiKitAutoLog`), you log your own data exactly the same way you would in a manual PsiKit session: call `Logger.recordOutput(...)` (and optionally `Logger.recordMetadata(...)`).

```java
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.Logger;

@TeleOp(name = "Log Outputs Example")
public class LogOutputsExample extends OpMode {

    private long loopCount = 0;
    private double lastLoopTimeSec = 0.0;

    @Override
    public void init() {
        // Metadata is usually "once per run" (init is a good place)
        Logger.recordMetadata("Robot", "Decode");
        Logger.recordMetadata("Mode", "TeleOp");
    }

    @Override
    public void loop() {
        // NOTE: gamepad inputs are already autologged by PsiKit.
        // A common pattern is to log *derived* values (commands, setpoints, errors, state).

        double nowSec = getRuntime();
        double dtSec = nowSec - lastLoopTimeSec;
        lastLoopTimeSec = nowSec;
        loopCount++;

        // Simple primitives
        Logger.recordOutput("Loop/Count", loopCount);
        Logger.recordOutput("Loop/TimeSec", nowSec);
        Logger.recordOutput("Loop/DTms", dtSec * 1000.0);

        // Derived commands (still useful even if you don't log raw joystick axes)
        double driveCmd = -gamepad1.left_stick_y;
        double turnCmd = gamepad1.right_stick_x;
        Logger.recordOutput("Drive/Cmd/Forward", driveCmd);
        Logger.recordOutput("Drive/Cmd/Turn", turnCmd);

        // Example: a state string based on your state machine
        String state = (nowSec < 2.0) ? "SPINUP" : "RUN";
        Logger.recordOutput("Robot/State", state);
    }
}
```

### Opt out a specific OpMode

Add `@PsiKitNoAutoLog` to any OpMode that you *don’t* want instrumented.

```java
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.ftc.autolog.PsiKitNoAutoLog;

@TeleOp(name = "No Logging")
@PsiKitNoAutoLog
public class NoLoggingTeleOp extends OpMode {
    @Override public void init() {}
    @Override public void loop() {}
}
```

---

## 2) Opt in a single OpMode, and change the RLOG port

For **opt-in only** behavior (the default), keep global logging disabled and annotate the OpModes you want.

### Keep global disabled, then opt in
<!-- tabs:start -->

#### **Kotlin**

```kotlin
import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings

object PsiKitConfig {
    @JvmStatic
    @OnCreateEventLoop
    fun configure(context: Context, ftcEventLoop: FtcEventLoop) {
        PsiKitAutoLogSettings.enabledByDefault = false
    }
}
```

#### **Java**

```java
package org.firstinspires.ftc.teamcode;

import android.content.Context;

import com.qualcomm.ftccommon.FtcEventLoop;

import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogSettings;

public final class PsiKitConfig {
    @OnCreateEventLoop
    public static void configure(Context context, FtcEventLoop ftcEventLoop) {
        PsiKitAutoLogSettings.enabledByDefault = false;
    }
}
```

<!-- tabs:end -->

### Annotate a single iterative OpMode

```java
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

@TeleOp(name = "Logged TeleOp")
@PsiKitAutoLog(rlogPort = 5900)
public class LoggedTeleOp extends OpMode {
    @Override public void init() {}

    @Override public void loop() {
        telemetry.addData("Status", "Hello");
        telemetry.update();
    }
}
```

### Example: logging your own outputs (single opt-in)

Same logging calls — the only difference is that you opt in via `@PsiKitAutoLog(...)`.

You can also change folder/filename per OpMode:

```java
@PsiKitAutoLog(
    rlogPort = 5900,
    rlogFolder = "/sdcard/FIRST/PsiKit/",
    rlogFilename = "my-run.rlog"
)
```

---

## 3) Linear OpModes

### 3a) Opt in a LinearOpMode (limited)

You *can* annotate a `LinearOpMode` directly. PsiKit will start a logging session at **pre-start** and end it at **post-stop**.

However, FTC’s notifier callbacks don’t provide a safe way to wrap the *per-loop* linear execution, so this mode is **limited** (no per-loop wrapper).

If you want per-loop PsiKit ticking **without** switching to a PsiKit base class, you can call helper methods from your loop (see next section).

```java
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

@TeleOp(name = "Linear (session only)")
@PsiKitAutoLog
public class LinearSessionOnly extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        waitForStart();
        while (opModeIsActive()) {
            telemetry.addLine("Running");
            telemetry.update();
            idle();
        }
    }
}
```

### 3b) Annotated LinearOpMode, with explicit per-loop ticking (recommended)

If you annotate a plain `LinearOpMode`, PsiKit will start/end the session automatically — but you can also add per-loop calls to get bulk-cache + input logging behavior similar to the iterative wrapper.

What these calls do:
- `PsiKitAutoLogger.linearPeriodicBeforeUser(this)` runs `Logger.periodicBeforeUser()` and (if the auto-log session is active) `FtcLoggingSession.logOncePerLoop(...)`.
- `PsiKitAutoLogger.linearPeriodicAfterUser(userCodeSec, psiKitOverheadSec)` records `LoggedRobot/UserCodeMS`, `LoggedRobot/LogPeriodicMS`, etc. Both values are durations in **seconds**.

<!-- tabs:start -->

#### **Java**

```java
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogger;

@TeleOp(name = "Linear (annotated + explicit tick)")
@PsiKitAutoLog
public class LinearAnnotatedTicked extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        long loopCount = 0;
        waitForStart();

        while (opModeIsActive()) {
            double beforePsiKitStart = Logger.getRealTimestamp();
            PsiKitAutoLogger.linearPeriodicBeforeUser(this);
            double beforeUserEnd = Logger.getRealTimestamp();

            // ---- your loop body (reads hardware, computes, writes outputs) ----
            Logger.recordOutput("Loop/RuntimeSec", getRuntime());
            Logger.recordOutput("Loop/Count", ++loopCount);

            // Example: log derived command + setpoint
            double forwardCmd = -gamepad1.left_stick_y;
            double targetSpeed = forwardCmd * 2500.0; // ticks/s or rpm, your units
            Logger.recordOutput("Drive/Cmd/Forward", forwardCmd);
            Logger.recordOutput("Shooter/TargetSpeed", targetSpeed);

            // Example: battery voltage
            double volts = 0.0;
            if (hardwareMap.voltageSensor != null && hardwareMap.voltageSensor.iterator().hasNext()) {
                volts = hardwareMap.voltageSensor.iterator().next().getVoltage();
            }
            Logger.recordOutput("Robot/BatteryVolts", volts);

            telemetry.addData("t", getRuntime());
            telemetry.update();

            PsiKitAutoLogger.linearPeriodicAfterUser(
                Logger.getRealTimestamp() - beforeUserEnd,
                beforeUserEnd - beforePsiKitStart
            );

            idle();
        }
    }
}
```

#### **Kotlin**

```kotlin
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogger

@TeleOp(name = "Linear (annotated + explicit tick)")
@PsiKitAutoLog
class LinearAnnotatedTicked : LinearOpMode() {
    override fun runOpMode() {
        var loopCount = 0L
        waitForStart()

        while (opModeIsActive()) {
            val beforePsiKitStart = Logger.getRealTimestamp()
            PsiKitAutoLogger.linearPeriodicBeforeUser(this)
            val beforeUserEnd = Logger.getRealTimestamp()

            // ---- your loop body (reads hardware, computes, writes outputs) ----
            Logger.recordOutput("Loop/RuntimeSec", runtime)
            Logger.recordOutput("Loop/Count", ++loopCount)

            val forwardCmd = -gamepad1.left_stick_y
            val targetSpeed = forwardCmd * 2500.0
            Logger.recordOutput("Drive/Cmd/Forward", forwardCmd)
            Logger.recordOutput("Shooter/TargetSpeed", targetSpeed)

            val volts = hardwareMap.voltageSensor?.iterator()?.takeIf { it.hasNext() }?.next()?.voltage ?: 0.0
            Logger.recordOutput("Robot/BatteryVolts", volts)

            telemetry.addData("t", runtime)
            telemetry.update()

            PsiKitAutoLogger.linearPeriodicAfterUser(
                Logger.getRealTimestamp() - beforeUserEnd,
                beforeUserEnd - beforePsiKitStart,
            )

            idle()
        }
    }
}
```

<!-- tabs:end -->

### 3d) Annotated LinearOpMode, using `PsiKitAutoLogger.linearLoop(...)`

If you always write your linear code as `while (opModeIsActive()) { ... }`, you can let PsiKit own that loop and automatically tick logging each iteration.

<!-- tabs:start -->

#### **Java**

```java
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogger;

@TeleOp(name = "Linear (annotated + linearLoop)")
@PsiKitAutoLog
public class LinearAnnotatedLoop extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        long loopCount = 0;
        waitForStart();

        PsiKitAutoLogger.linearLoop(this, new Runnable() {
            @Override
            public void run() {
                Logger.recordOutput("Loop/RuntimeSec", getRuntime());
                Logger.recordOutput("Loop/Count", ++loopCount);

                telemetry.addLine("Running");
                telemetry.update();
                idle();
            }
        });
    }
}
```

#### **Kotlin**

```kotlin
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLogger

@TeleOp(name = "Linear (annotated + linearLoop)")
@PsiKitAutoLog
class LinearAnnotatedLoop : LinearOpMode() {
    override fun runOpMode() {
        var loopCount = 0L
        waitForStart()

        PsiKitAutoLogger.linearLoop(this) {
            Logger.recordOutput("Loop/RuntimeSec", runtime)
            Logger.recordOutput("Loop/Count", ++loopCount)

            telemetry.addLine("Running")
            telemetry.update()
            idle()
        }
    }
}
```

<!-- tabs:end -->

---

## 4) Useful patterns

### Avoid double-logging when mixing styles

If you already extend a PsiKit base class like `PsiKitIterativeOpMode` or `PsiKitLinearOpMode`, you generally should **not** also rely on the wrapper instrumentation.

PsiKit’s base classes are already annotated with `@PsiKitNoAutoLog`, but if you create your own wrappers/subclasses, you can add `@PsiKitNoAutoLog` yourself.

### Change logging behavior per OpMode without changing global defaults

Even with global logging enabled, you can still override per-opmode parameters using `@PsiKitAutoLog(...)`.

Example: change port for one OpMode while leaving global on:

```java
@TeleOp(name = "Global On, Custom Port")
@PsiKitAutoLog(rlogPort = 6000)
public class CustomPortTeleOp extends OpMode {
    @Override public void init() {}
    @Override public void loop() {}
}
```
