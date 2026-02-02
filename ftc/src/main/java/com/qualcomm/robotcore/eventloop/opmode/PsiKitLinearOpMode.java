package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.FtcLoggingSession;
import org.psilynx.psikit.ftc.autolog.PsiKitNoAutoLog;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * LinearOpMode-compatible base class that automatically runs PsiKit logging.
 *
 * <p>Usage: extend this class instead of {@link LinearOpMode} and implement {@link #runOpMode()}.
 * Your existing linear-style code (waitForStart/opModeIsActive/sleep/idle) should continue to work.
 */
@PsiKitNoAutoLog
public abstract class PsiKitLinearOpMode extends OpMode {

    private volatile boolean userMethodReturned = false;
    private volatile boolean userMonitoredForStart = false;
    private final Object runningNotifier = new Object();

    private final FtcLoggingSession psiKitSession = new FtcLoggingSession();
    private volatile boolean psiKitStarted = false;

    private volatile long eventLoopCounter = 0;
    private long lastTickCounter = -1;

    private boolean timingInitialized = false;
    private double lastUserIntervalStart = 0.0;
    private double lastPeriodicBeforeLen = 0.0;

    /** Override to change the optional RLOG server port. Return 0 to disable. */
    public int getRlogPort() {
        return 5800;
    }

    /** Override to change the output folder for RLOGWriter. */
    public String getRlogFolder() {
        return "/sdcard/FIRST/PsiKit/";
    }

    /** Override to specify a filename; empty means "use default". */
    public String getRlogFilename() {
        return "";
    }

    /** Called after Logger.reset() and receiver setup, but before Logger.start(). */
    protected void onPsiKitConfigureLogging() {
        // optional
    }

    /** Override to control whether PsiKit logs Pinpoint odometry (when present) under `/Odometry/...`. */
    protected boolean enablePinpointOdometryLogging() {
        return true;
    }

    /** Override this method and place your code here. */
    public abstract void runOpMode() throws InterruptedException;

    /** Pauses until START is pressed or the OpMode is interrupted/stopped. */
    public void waitForStart() {
        while (!isStarted()) {
            synchronized (runningNotifier) {
                try {
                    runningNotifier.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // In replay, START/STOP state is driven by log playback inside logOncePerLoop().
            // Ticking here allows standard LinearOpMode patterns (waitForStart() without an init loop)
            // to advance replay state.
            psiKitTickOncePerEventLoopIteration();
        }
    }

    /** Yields execution to allow other threads to run. */
    public final void idle() {
        Thread.yield();
    }

    /** Sleeps for the given number of milliseconds, or until interrupted. */
    public final void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** True when in RUN (started and not stop requested). */
    public final boolean opModeIsActive() {
        boolean isActive = !isStopRequested() && isStarted();
        if (isActive) {
            psiKitTickOncePerEventLoopIteration();
            idle();
        }
        return isActive;
    }

    /** True when in INIT (not started and not stop requested). */
    public final boolean opModeInInit() {
        boolean inInit = !isStarted() && !isStopRequested();
        if (inInit) {
            psiKitTickOncePerEventLoopIteration();
        }
        return inInit;
    }

    /** True once START is pressed (or if interrupted). */
    public final boolean isStarted() {
        if (isStarted) {
            userMonitoredForStart = true;
        }
        return this.isStarted || Thread.currentThread().isInterrupted();
    }

    /** True if stop has been requested (or if interrupted). */
    public final boolean isStopRequested() {
        return this.stopRequested || Thread.currentThread().isInterrupted();
    }

    /** This method may not be overridden by linear opmodes. */
    @Override
    public final void init() {
    }

    /** This method may not be overridden by linear opmodes. */
    @Override
    public final void init_loop() {
    }

    /** This method may not be overridden by linear opmodes. */
    @Override
    public final void start() {
    }

    /** This method may not be overridden by linear opmodes. */
    @Override
    public final void loop() {
    }

    /** This method may not be overridden by linear opmodes. */
    @Override
    public final void stop() {
    }

    // Package-private, called on the OpModeThread when the OpMode is initialized
    @Override
    final void internalRunOpMode() throws InterruptedException {
        userMethodReturned = false;
        userMonitoredForStart = false;

        psiKitSession.enablePinpointOdometryLogging = enablePinpointOdometryLogging();

        final Function0<Unit> configure = new Function0<Unit>() {
            @Override
            public Unit invoke() {
                onPsiKitConfigureLogging();
                return Unit.INSTANCE;
            }
        };

        final int port = getRlogPort();
        final String folder = getRlogFolder();
        final String filename = getRlogFilename();

        if (filename != null && !filename.isEmpty()) {
            psiKitSession.start(this, port, filename, folder, null, this, configure);
        } else {
            psiKitSession.startWithConfigure(this, port, folder, null, configure);
        }
        psiKitStarted = true;

        try {
            runOpMode();
        } finally {
            userMethodReturned = true;

            // In linear style we infer user time as the time between PsiKit tick boundaries.
            // Flush the final partially-completed interval so UserCodeMS isn't stuck at 0.
            psiKitFinalizeTiming();

            try {
                psiKitSession.end();
            } catch (Exception ignored) {
                // ignore
            }
            RobotLog.d("User runOpMode() exited");
            requestOpModeStop();
        }
    }

    // Package-private, called on the main event loop thread
    @Override
    final void internalOnStart() {
        synchronized (runningNotifier) {
            runningNotifier.notifyAll();
        }
    }

    // Package-private, called on the main event loop thread
    @Override
    final void internalOnEventLoopIteration() {
        time = getRuntime();

        synchronized (runningNotifier) {
            runningNotifier.notifyAll();
        }

        if (telemetry instanceof TelemetryInternal) {
            ((TelemetryInternal) telemetry).tryUpdateIfDirty();
        }

        eventLoopCounter++;
    }

    // Package-private, called on the main event loop thread
    @Override
    final void internalOnStopRequested() {
        if (!userMonitoredForStart && userMethodReturned) {
            RobotLog.addGlobalWarningMessage(
                    "The OpMode which was just initialized ended prematurely as a result of not monitoring for the start condition. Did you forget to call waitForStart()?"
            );
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    void newGamepadDataAvailable(Gamepad latestGamepad1Data, Gamepad latestGamepad2Data) {
        gamepad1.copy(latestGamepad1Data);
        gamepad2.copy(latestGamepad2Data);
    }

    private void psiKitTickOncePerEventLoopIteration() {
        if (!psiKitStarted) {
            return;
        }

        long currentCounter = eventLoopCounter;
        if (currentCounter == lastTickCounter) {
            return;
        }
        lastTickCounter = currentCounter;

        // Close previous interval (user code ran between tick boundaries).
        double now = Logger.getRealTimestamp();
        if (timingInitialized) {
            double userLen = 0.0;
            if (!this.stopRequested) {
                userLen = Math.max(0.0, now - lastUserIntervalStart);
            }
            Logger.periodicAfterUser(userLen, lastPeriodicBeforeLen);
        }

        // Open next interval.
        double before = Logger.getRealTimestamp();
        Logger.periodicBeforeUser();
        psiKitSession.logOncePerLoop(this);
        double after = Logger.getRealTimestamp();
        lastPeriodicBeforeLen = Math.max(0.0, after - before);
        lastUserIntervalStart = Logger.getRealTimestamp();
        timingInitialized = true;
    }

    private void psiKitFinalizeTiming() {
        if (!psiKitStarted || !timingInitialized) {
            return;
        }

        double now = Logger.getRealTimestamp();
        double userLen = 0.0;
        if (!this.stopRequested) {
            userLen = Math.max(0.0, now - lastUserIntervalStart);
        }
        Logger.periodicAfterUser(userLen, lastPeriodicBeforeLen);

        timingInitialized = false;
        lastUserIntervalStart = 0.0;
        lastPeriodicBeforeLen = 0.0;
    }
}
