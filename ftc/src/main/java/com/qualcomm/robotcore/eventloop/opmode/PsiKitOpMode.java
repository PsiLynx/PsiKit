package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.FtcLoggingSession;
import org.psilynx.psikit.ftc.autolog.PsiKitNoAutoLog;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * OpMode-compatible base class that automatically runs PsiKit logging.
 *
 * <p>Usage: extend this class instead of {@link OpMode} and implement the normal iterative
 * lifecycle methods ({@link #init()}, {@link #init_loop()}, {@link #start()}, {@link #loop()},
 * {@link #stop()}).
 *
 * <p>Unlike {@link org.psilynx.psikit.ftc.PsiKitIterativeOpMode} (Kotlin), this class lives in the
 * FTC SDK package so it can override package-private internal hooks and does not require user code
 * to call {@code super} to keep logging ticking.
 */
@PsiKitNoAutoLog
public abstract class PsiKitOpMode extends OpMode {

    private final FtcLoggingSession psiKitSession = new FtcLoggingSession();
    private volatile boolean psiKitStarted = false;

    // Latest gamepad data (used to update gamepad1 and gamepad2 in between user code callbacks)
    private volatile Gamepad latestGamepad1Data = new Gamepad();
    private volatile Gamepad latestGamepad2Data = new Gamepad();

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

    //----------------------------------------------------------------------------------------------
    // OpModeInternal hooks
    //----------------------------------------------------------------------------------------------

    // Package-private, called on the OpModeThread when the OpMode is initialized
    @Override
    final void internalRunOpMode() throws InterruptedException {
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
            // Until we delete the deprecated hooks entirely, we keep calling them.
            internalPreInit();

            // Match the OpMode semantics but wrap each callback with PsiKit ticking.
            tickAndCallUserCallback(UserCallback.INIT);

            while (!isStarted && !stopRequested) {
                tickAndCallUserCallback(UserCallback.INIT_LOOP);
                // Until we delete the deprecated hooks entirely, we keep calling them.
                internalPostInitLoop();

                //noinspection BusyWait
                Thread.sleep(1);
            }

            if (isStarted) {
                tickAndCallUserCallback(UserCallback.START);

                while (!stopRequested) {
                    tickAndCallUserCallback(UserCallback.LOOP);
                    // Until we delete the deprecated hooks entirely, we keep calling them.
                    internalPostLoop();

                    //noinspection BusyWait
                    Thread.sleep(1);
                }
            }

            tickAndCallUserCallback(UserCallback.STOP);
        } finally {
            try {
                psiKitSession.end();
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    @Override
    final void newGamepadDataAvailable(Gamepad latestGamepad1Data, Gamepad latestGamepad2Data) {
        // Save the gamepad data for use after the current user method finishes running
        this.latestGamepad1Data = latestGamepad1Data;
        this.latestGamepad2Data = latestGamepad2Data;
    }

    //----------------------------------------------------------------------------------------------
    // Internal
    //----------------------------------------------------------------------------------------------

    private enum UserCallback {
        INIT,
        INIT_LOOP,
        START,
        LOOP,
        STOP
    }

    private void internalPreUserCode() {
        // Keep OpMode.time consistent with SDK behavior.
        time = getRuntime();

        // We copy the gamepad data instead of replacing the gamepad instances because the gamepad
        // instances may contain queued effect data.
        if (gamepad1 != null) {
            gamepad1.copy(latestGamepad1Data);
        }
        if (gamepad2 != null) {
            gamepad2.copy(latestGamepad2Data);
        }
    }

    private void internalPostUserCode() {
        if (telemetry != null) {
            telemetry.update();
        }
    }

    private void tickAndCallUserCallback(UserCallback which) {
        internalPreUserCode();

        if (psiKitStarted) {
            double beforeStart = Logger.getRealTimestamp();
            Logger.periodicBeforeUser();
            psiKitSession.logOncePerLoop(this);
            double beforeEnd = Logger.getRealTimestamp();
            double periodicBeforeLen = beforeEnd - beforeStart;

            double userStart = Logger.getRealTimestamp();
            switch (which) {
                case INIT:
                    init();
                    break;
                case INIT_LOOP:
                    init_loop();
                    break;
                case START:
                    start();
                    break;
                case LOOP:
                    loop();
                    break;
                case STOP:
                    stop();
                    break;
            }
            double userEnd = Logger.getRealTimestamp();

            Logger.periodicAfterUser(userEnd - userStart, periodicBeforeLen);
        } else {
            // Should be unreachable, but keep behavior sensible if session startup fails.
            switch (which) {
                case INIT:
                    init();
                    break;
                case INIT_LOOP:
                    init_loop();
                    break;
                case START:
                    start();
                    break;
                case LOOP:
                    loop();
                    break;
                case STOP:
                    stop();
                    break;
            }
        }

        internalPostUserCode();
    }
}
