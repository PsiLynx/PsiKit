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
2. Check the [supported device list](supported.md) to make sure that all of the hardware on your robot has supported wrappers
3. Set up your OpMode loop to make sure that hardware gets processed, following the example OpMode on the [usage](usage.md) page

## FTC replay in practice

In FTC, the most common workflow is:

1. Record logs on the robot (writes `.rlog` files under `/storage/emulated/0/FIRST/PsiKit`).
2. Copy a target `.rlog` to your PC.
3. Run a desktop/JVM replay that reads the `.rlog` and optionally produces a *new* `.rlog` with additional outputs.
4. Open the replay output `.rlog` in AdvantageScope and compare `RealOutputs/...` vs `ReplayOutputs/...`.

For copying logs from a Control Hub / RC, see:

- [FTC: ADB and RLOG runbook](ftc-adb-and-rlog-runbook.md)

PsiKit contains the low-level replay reader (`RLOGReplay`) which updates a `LogTable` over time. Many teams build a small “replay runner” in their robot project (often using Robolectric) so they can instantiate an OpMode and re-run their code against recorded inputs.

## Troubleshooting

- If replay silently falls back to a minimal loop, ensure your project logs a clear marker (e.g., `ReplayOnly/LoopCount`) and/or records init exceptions into the log as strings.
