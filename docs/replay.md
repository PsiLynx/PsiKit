# Replay

Replay is a very powerful feature in Psi Kit. It allows you to record all the inputs your code receives, and then replay the code from a log file. This allows you to replay the **exact internal state of the robot code**, as it was while the robot was running

### Common Use Cases:
* Replay logs from when a bug occurred, and:
  - Log additional values such as internal state in a calculation
  - Use the IDE debugger to step through the robot code
* Record a log of matches, when live streaming the data is not allowed
* Keep debugging the code when the pesky "*build team*" takes away the robot

### Usage:
1. Use `Logger.addDataReceiver(new RLOGWriter(String fileName))` to record data to a log file 
2. Check the [list of supported I2C devices](/supportedI2c.md) to make sure that all of the hardware on your robot has supported wrappers
3. Set up your OpMode loop to make sure that hardware gets processed, following the example OpMode on the [usage](/usage.md) page
