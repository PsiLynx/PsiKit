# Usage Guide
## Minimum Configuration
The class you will interact the most with is `Logger`. It acts as a manager for all the I/O going on.

1. During OpMode initialization, create an `RLOGServer` and call it's `start()` 
   method
2. During OpMode initialization, add that `RLOGServer` to the Logger using
   `Logger.addDataReceiver(server)` this will serve the data
   from the robot, allowing it to be available on your computer.
> Note that the RLOG Server **must** be disabled during competition to be legal.

3. Optionally, start recording to a log file using the same process as above
   but with an `RLOGWriter`
> This may be run during competition matches, for later viewing

4. Add any metadata with `Logger.recordMetadata(String key, String value)`

> If, for some reason, you cannot subclass `PsiKitOpMode`, please look 
> through it's code to see what additional methods you must call

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

Returns the current time in seconds since `Logger.start()` was called. Currently just uses `System.nanoTime()`, but in the future, using that function as your time source will be very important in order to proper replay data. 

### Classes such as `Pose2d` and `LoggedMechanism2d`

Most classes referenced in the advantage scope docs are available in Psi Kit, ones that are part of WPI are in `psikit.wpi.*`.
___

**The [AdvantageScope Tab Reference](https://docs.advantagescope.org/category/tab-reference) is a very good resource; things that work the same in Psi Kit as in the AdvantageKit examples will not be covered by these docs.**

## Next, [Start Using Replay](/replay.md)

### Example OpMode
This example OpMode has everything necessary to run the
Psi Kit live data server, and log data for replay later.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.psilynx.psikit.core.rlog.RLOGServer;
import org.psilynx.psikit.core.rlog.RLOGWriter;
import org.psilynx.psikit.core.Logger;

import org.psilynx.psikit.ftc.PsiKitOpMode;

@TeleOp(name="ConceptPsiKitLogger")
class ConceptPsiKitLogger extends PsiKitOpMode {
    @Override
    public void psiKit_init() {
        var server = new RLOGServer();
        var writer = new RLOGWriter();

        Logger.addDataReceiver(new RLOGServer());
        Logger.addDataReceiver(new RLOGWriter("log.rlog"));

        Logger.recordMetadata("some metadata", "string value");
        //examples
    }
    public void psiKit_init_loop() {
        /*
          
         init loop logic goes here
          
        */
    }
    @Override
    public void psiKit_start() {
        // start logic here
    }
    @Override
    public void psiKit_loop() {
 
        /*
          
         OpMode logic goes here
           
        */

        Logger.recordOutput("OpMode/example", 2.0);
        // example

    }
    @Override
    public void psiKit_end() {
        // end logic goes here
    }
}
```
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

PsiKit can automatically start/end a session using an OpMode wrapper. See [README-autolog.md](README-autolog.md) for examples.

For `LinearOpMode`, you can optionally add explicit per-loop ticking via `PsiKitAutoLogger.linearPeriodicBeforeUser(...)` and `PsiKitAutoLogger.linearPeriodicAfterUser(...)` (see `ftc-autolog-examples.md`).
## Next, [Install Advantage Scope](installAscope.md)
