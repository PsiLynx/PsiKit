package org.psilynx.psikit.ftc

/**
 * Global knobs for reducing logging overhead.
 *
 * Notes on semantics:
 * - Skipping `table.put(...)` for a key does NOT delete it; LogTable retains the last value.
 * - This makes rate limiting safe: "in between" loops will still observe the last logged value.
 */
object FtcLogTuning {
    /** If true, issue one bulk read per hub at the start of each loop (after clearBulkCache). */
    @JvmField var prefetchBulkDataEachLoop: Boolean = false

    /**
     * If > 0, wrappers may sample *non-bulk* (generally I2C / ADC / readback) data at this period
     * and skip writes in between.
     */
    @JvmField var nonBulkReadPeriodSec: Double = 0.0

    /** If true, PsiKit will log IMU values when the IMU is present in the HardwareMap. */
    @JvmField var logImu: Boolean = true

    /**
     * If true, PsiKit will sample color/distance sensors in the background (during
     * HardwareMap processing) and cache values inside the wrapper.
     *
     * If false, the wrapper will avoid background I2C reads. Live-mode user code will still get
     * fresh values via on-demand passthrough reads.
     */
    @JvmField var processColorDistanceSensorsInBackground: Boolean = true

    /**
     * If true, motor current (typically a non-bulk Lynx read) will be logged.
     *
     * This is intentionally separate from [nonBulkReadPeriodSec] because motor current tends to be
     * much more expensive than other non-bulk reads, and is often only needed at a slow rate.
     */
    @JvmField var logMotorCurrent: Boolean = false

    /**
     * Period (seconds) for sampling motor current when [logMotorCurrent] is enabled.
     * Typical values: 0.05 (50ms) or 0.1 (100ms).
     */
    @JvmField var motorCurrentReadPeriodSec: Double = 0.1

    /**
     * If > 0, Pinpoint odometry updates (I2C reads) will occur at this period.
     * The last pose is still written to the log each loop.
     */
    @JvmField var pinpointReadPeriodSec: Double = 0.0

    /**
     * If true (default), PsiKit will call Pinpoint `update()` when sampling for logs.
     *
     * If your robot code already calls Pinpoint update each loop (e.g. a motion follower
     * localizer), set this false to avoid double I2C transactions in the same loop.
     * PsiKit will still log the current pose each loop.
     */
    @JvmField var pinpointLoggerCallsUpdate: Boolean = true

    /**
     * If true and the Pinpoint device supports it (firmware V3+ for PsiKit's driver), configure a
     * smaller bulk read scope (status + loopTime + x/y/heading). Reduces I2C payload size.
     */
    @JvmField var pinpointUseMinimalBulkReadScope: Boolean = false

    /**
     * If true, PinpointWrapper (HardwareMapWrapper path) will also publish `/Odometry/<name>`.
     *
     * When logging odometry from a motion follower (recommended), set this false to avoid
     * duplicated or conflicting `/Odometry` sources.
     */
    @JvmField var pinpointWrapperPublishesOdometry: Boolean = false

    /**
     * If true, [PedroFollowerOdometryLogger] will also publish `/Odometry/<name>` and
     * `/Odometry/<name>/PedroInches` in addition to the canonical `/Odometry` paths.
     */
    @JvmField var pedroFollowerPublishesNamedOdometry: Boolean = false
}
