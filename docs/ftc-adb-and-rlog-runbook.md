# FTC ADB + PsiKit `.rlog` Runbook (Windows)

This is a practical checklist for pulling logs from an FTC Control Hub / Robot Controller over ADB and validating PsiKit `.rlog` recordings.

## Assumptions

- You have ADB installed (`adb` on PATH).
- You are connected to the Control Hub network.
- Typical Control Hub ADB endpoint: `192.168.43.1:5555`.

## ADB connection (reliable sequence)

PowerShell:

- `adb kill-server`
- `adb start-server`
- `adb disconnect`
- `adb connect 192.168.43.1:5555`
- `adb devices -l`

If `adb connect` works but `adb shell` hangs intermittently, retry after `adb kill-server`.

## Deploy updated code to the robot (ADB install)

If you’re iterating on code and want to push a new Robot Controller APK via ADB, this is the most reliable CLI path.

### Build the app APK

In the `2025_Decode` repo layout used by PsiKit-based projects, the **app APK** is typically produced by `:TeamCode` (and `:FtcRobotController` is an Android library), so build:

- `cd c:\code\TeraBridges\2025_Decode`
- `./gradlew.bat :TeamCode:assembleDebug --no-daemon`

### Install the app APK

- `adb install -r .\TeamCode\build\outputs\apk\debug\TeamCode-debug.apk`

### Restart Robot Controller (optional)

- `adb shell am force-stop com.qualcomm.ftcrobotcontroller`
- `adb shell am start -n com.qualcomm.ftcrobotcontroller/org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity`

### Verify you actually updated the installed app (recommended)

Check the on-device `base.apk` path and timestamp/size:

- `adb shell pm path com.qualcomm.ftcrobotcontroller`
- `adb shell ls -l /data/app/<whatever>/base.apk`

If the `ls -l` size/timestamp doesn’t change after an install, you probably installed an `androidTest` APK (common when running `:FtcRobotController:installDebug` in repo layouts where `FtcRobotController` is a library).

## Locate PsiKit logs on device

PsiKit logs are usually under the shared storage tree:

- `/storage/emulated/0/FIRST/PsiKit`

Useful discovery commands:

- `adb shell "ls -la /storage/emulated/0/FIRST"`
- `adb shell "ls -la /storage/emulated/0/FIRST/PsiKit"`

To list all candidate `.rlog` files by modification time:

- `adb shell "find /storage/emulated/0 -type f -name '*.rlog' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 50"`

If `find` is missing features on your Android build, fall back to:

- `adb shell "find /storage/emulated/0 -type f -name '*.rlog' 2>/dev/null | head -n 200"`

## Pulling files to your PC (PowerShell)

Create a local folder first (example):

- `New-Item -ItemType Directory -Force c:\code\TeraBridges\2025_Decode\build\psikitReplayOut | Out-Null`

Pull a specific `.rlog`:

- `adb pull /storage/emulated/0/FIRST/PsiKit/MainTeleopPsikit_log_YYYYMMDD_HHMMSS_mmm.rlog c:\code\TeraBridges\2025_Decode\build\psikitReplayOut\`

Tip: if you’re unsure of the exact name, copy-paste from the `find` output.

## Alternative: Android Studio Device Explorer (no ADB commands)

If you use Android Studio’s **Device Explorer** to download a file from the Control Hub, Android Studio caches it under a Windows path like:

- `C:\Users\<you>\AppData\Local\Google\AndroidStudio<version>\device-explorer\REV Robotics Control Hub v1.0\_\sdcard\FIRST\PsiKit\...`

If the `.rlog` exists at that cached path, you can replay it directly from there (no need to `adb pull` it again).

## Pull Robot Controller logs

Two common sources:

- App log directory (varies by SDK/app)
- Logcat dump (always available)

Logcat dump to a file:

- `adb logcat -d -b all -v threadtime > c:\code\TeraBridges\2025_Decode\build\psikitReplayOut\logcat_YYYYMMDD_HHMMSS.txt`

If you want only PsiKit/FTC-ish lines (quick triage):

- `adb logcat -d -v threadtime | findstr /i "psikit GoBildaPinpointDriver Pinpoint LynxNackException FATAL EXCEPTION" > c:\path\logcat_filtered.txt`

## What “good Pinpoint logging” looks like

In a healthy run you should see (at least once):

- A hardware map wrap line for Pinpoint
- `/Odometry/<name>` Pose2d/Pose3d structs being logged
- Raw Pinpoint fields under `HardwareMap/<name>/...`

Example keys to expect in the `.rlog`:

- `/Odometry/pinpoint/Pose2d`
- `/Odometry/pinpoint/Pose3d`
- `HardwareMap/pinpoint/deviceId`
- `HardwareMap/pinpoint/xPosition`, `yPosition`, `hOrientation`
- `HardwareMap/pinpoint/xVelocity`, `yVelocity`, `hVelocity`

## Decoding/inspecting `.rlog`

A `.rlog` is a binary stream; plain text search won’t work.

PsiKit already contains the decoder (`org.psilynx.psikit.core.rlog.RLOGDecoder` and `RLOGReplay`).

### Option A: Quick one-off JUnit helper (recommended for local debugging)

Create a temporary JUnit test that:

- Opens a path from `RLOG_PATH`
- Reads N entries
- Prints distinct keys (and asserts `/Odometry/...` exists)

Then run:

- `cd c:\code\psilynx\PsiKit`
- `$env:RLOG_PATH='c:\path\to\file.rlog'`
- `.\gradlew.bat :core:test --no-daemon --tests <YourTestClass> --info --rerun-tasks`

Notes:

- `--rerun-tasks` forces stdout even when Gradle thinks the test is up-to-date.
- `--info` ensures test standard output is displayed.
- If you're re-running a test that *creates* a new `.rlog` (e.g., replay-output logs), always verify a new file was actually written (check the newest filename + modified time in the output directory).
	- If no new `.rlog` appears, run again with `--rerun-tasks` (and/or disable configuration cache) so Gradle can't skip the test.
	- On Windows, `dir <outputDir>\*.rlog | sort LastWriteTime` is a quick sanity check.

### Option B: Add a tiny CLI `main()` (if you want a permanent tool)

If you prefer a permanent command-line utility, add a `public static void main(String[] args)` in `core` that:

- Accepts `.rlog` path
- Prints key summary

Then wire a Gradle `JavaExec` task to run it.

## Common gotchas

- Workspace searches often exclude `build/` folders, so text searches may show “no matches” until you enable searching ignored files.
- Network ADB to Control Hub can be flaky; restarting the ADB server helps.
- The newest `.rlog` is not always the one you want; sort by timestamp and verify the filename matches your OpMode.
