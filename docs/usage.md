# Usage Guide
### Minimum Configuration
The class you will interact the most with is `Logger`. 
It acts as a manager for all the I/O going on.

1. During Opmode initialization, add an `RLOGServer` to the Logger using
`Logger.addDataReceiver(new RLOGServer())` this will serve the data
from the robot, allowing it to be available on your computer.
> Note that the RLOG Server **must** be disabled during competition to be legal.

2. Add any metadata with `Logger.recordMetadata(String key, String value)`

3. Optionally, start recording to a log file using `Logger.addDataReceiver(new RLOGWriter(String folder, String fileName))`

4. Start the logger with `Logger.start()`. After this,
no new metadata or data receivers can be added.

5. During the opMode loop, you must call `Logger.periodicBeforeUser()`
at the beginning of every loop, and 
`Logger.periodicAfterUser(long userCodeLength, long periodicBeforeLength))`
at the end of every loop. 
   * `userCodeLength` is the number of seconds it took your code 
to run in between in beforeUser and afterUser
   * `periodicBeforeLength`is the number of seconds it took to run
`Logger.periodicBeforeUser()`. 
   * Passing 0 to either or both of these
is completely okay, they are only used to log additional information 
about how long things took. 

6. During stop, call `Logger.end()` so it can clean things up.

### Other Methods
In addition to the methods listed above, here are the most common ways
you will interact Psi Kit
___
> `Logger.recordOutput(String key, T value)`

Where T is any primitive, String, Enum, `LoggedMechanism2D`, or a class that implements 
`WPISerializable` or `StructSerializable`, or a 1-2D array of 
those types. This is how you make data available to advantage scope.
data structures like `Pose2d` implement `StructSerializable`, so you
can automatically use them. This method must be called once per loop
for the data to stay on advantageScope.
The logger has a concept of tables and subtables, and `key` is split
on `"/"` characters, and the pieces are used as subtables. for instance,
if you log `a` to `Foo/Bar` and `b` to `Foo/Baz`, you will see
```
Foo
├─ Bar   a
└─ Baz   b
```
It is recommended to log things in the same file in the same parent 
table, using the class name, for instance.
___
> `Logger.getTimestamp()`

Returns the current time in seconds since `Logger.start()` was called. 
Currently just uses `System.nanoTime()`, but in the future, using that
function as your time source will be very important in order to properly
replay data. 
___
> Classes such as `Pose2d` and `LoggedMechanism2d`

Most classes referenced in the advantage scope docs are available in 
Psi Kit, ones that are part of WPI are in `psikit.wpi.*`.
___

**The [AdvantageScope Tab Reference](https://docs.advantagescope.org/category/tab-reference)
is a very good resource, things that work the same in Psi Kit as in the 
AdvantageKit examples will not be covered by these docs.**

### Next, &nbsp;[Start Using Replay](/replay.md)

### Example Op Mode
This example opMode has everything necessary to run the 
Psi Kit live data server

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.psilynx.psikit.Logger;
import org.psilynx.psikit.io.RLOGServer;

@Teleop(name = "ConceptPsiKitLogger")
class ConceptPsiKitLogger {
   @Override
   public void init() {
      RLOGServer server = new RLOGServer();
      Logger.addDataReceiver(server);
      Logger.recordMetadata("opMode name", "ConceptPsiKitLogger");
      Logger.start(); // Start logging! No more data receivers, replay sources, or metadata values may be added.
      Logger.periodicAfterUser(0, 0);
   }

   @Override
   public void loop() {
      double beforeUserStart = Logger.getTimestamp();
      Logger.periodicBeforeUser();
      double beforeUserEnd = Logger.getTimestamp();


      // all logic goes here
      Logger.recordOutput("OpMode/example", 2.0); // example logging of an output


      double afterUserStart = Logger.getTimestamp();
      Logger.periodicAfterUser(
              afterUserStart - beforeUserEnd,
              beforeUserEnd - beforeUserStart
      );
   }
}
```
### Next, &nbsp;[Install Advantage Scope](installAscope.md)