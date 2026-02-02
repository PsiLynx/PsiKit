package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.ftc.FtcLogTuning

/**
 * Logs a Pedro follower pose (inches + radians) as AdvantageScope-friendly odometry.
 *
 * Canonical output:
 * - `/Odometry` with Pose2d/Pose3d (struct) in AdvantageScope Center/Rotated (meters + radians)
 *
 * Debug output:
 * - `/Odometry/PedroInches` with scalar `x`, `y`, `heading`, `headingDeg`
 *
 * Optional (disabled by default via [FtcLogTuning.pedroFollowerPublishesNamedOdometry]):
 * - `/Odometry/<name>` and `/Odometry/<name>/PedroInches`
 */
object PedroFollowerOdometryLogger {

    private const val INCH_TO_METER = 0.0254
    private const val FTC_FIELD_SIZE_IN = 144.0
    private const val FTC_FIELD_HALF_IN = FTC_FIELD_SIZE_IN / 2.0

    private val canonicalPose = StructPoseInputs("Pose2d", "Pose3d")
    private val pedroInchesScalars = PedroInchesScalarInputs()

    /**
     * Log a pose in Pedro coordinates (inches, radians).
     *
     * Pedro frame assumptions (from PedroCoordinates + Pose conventions):
     * - +X points toward the red alliance wall
     * - +Y is +90° CCW from +X
     *
     * Center/Rotated target assumptions (AdvantageScope):
     * - origin at field center
     * - +X points to the right from the red wall perspective
     *
     * Mapping (centered inches -> meters):
      * - xCenterRotated = -yCentered
      * - yCenterRotated = +xCentered
    * - headingCenterRotated = headingPedro + 90°
     */
    @JvmStatic
    fun log(name: String = "follower", xInches: Double, yInches: Double, headingRad: Double) {
        val xCenteredIn = xInches - FTC_FIELD_HALF_IN
        val yCenteredIn = yInches - FTC_FIELD_HALF_IN

          val xCenterRotatedMeters = -yCenteredIn * INCH_TO_METER
          val yCenterRotatedMeters = xCenteredIn * INCH_TO_METER
        val headingCenterRotatedRad = normalizeRadians(headingRad + Math.PI / 2.0)

        canonicalPose.set(xCenterRotatedMeters, yCenterRotatedMeters, headingCenterRotatedRad)
        Logger.processInputs("/Odometry", canonicalPose)

        if (FtcLogTuning.pedroFollowerPublishesNamedOdometry && name.isNotBlank()) {
            Logger.processInputs("/Odometry/$name", canonicalPose)
        }

        pedroInchesScalars.set(xInches, yInches, headingRad)
        Logger.processInputs("/Odometry/PedroInches", pedroInchesScalars)

        if (FtcLogTuning.pedroFollowerPublishesNamedOdometry && name.isNotBlank()) {
            Logger.processInputs("/Odometry/$name/PedroInches", pedroInchesScalars)
        }
    }

    private fun normalizeRadians(angleRad: Double): Double {
        var angle = angleRad
        while (angle >= Math.PI) angle -= 2.0 * Math.PI
        while (angle < -Math.PI) angle += 2.0 * Math.PI
        return angle
    }

    private class PedroInchesScalarInputs : LoggableInputs {
        private var xInches = 0.0
        private var yInches = 0.0
        private var headingRad = 0.0

        fun set(xInches: Double, yInches: Double, headingRad: Double) {
            this.xInches = xInches
            this.yInches = yInches
            this.headingRad = headingRad
        }

        override fun toLog(table: LogTable) {
            table.put("x", xInches)
            table.put("y", yInches)
            table.put("heading", headingRad)
            table.put("headingDeg", Math.toDegrees(headingRad))
        }

        override fun fromLog(table: LogTable) {
            // Optional: this is primarily used for recording, not replay.
        }
    }
}
