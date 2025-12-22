package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.LoggableInputs
import org.psilynx.psikit.core.wpi.math.Pose2d
import org.psilynx.psikit.core.wpi.math.Pose3d
import org.psilynx.psikit.core.wpi.math.Rotation2d

/**
 * Logs Pose2d/Pose3d as a single struct value each (so AdvantageScope can select the whole pose).
 */
class StructPoseInputs(
    private val pose2dKey: String,
    private val pose3dKey: String,
) : LoggableInputs {

    private var xMeters = 0.0
    private var yMeters = 0.0
    private var headingRad = 0.0

    fun set(xMeters: Double, yMeters: Double, headingRad: Double) {
        this.xMeters = xMeters
        this.yMeters = yMeters
        this.headingRad = headingRad
    }

    override fun toLog(table: LogTable) {
        if (pose2dKey.isNotBlank()) {
            val pose2d = Pose2d(xMeters, yMeters, Rotation2d(headingRad))
            table.put(pose2dKey, pose2d)
            removeLegacyPose2d(table, pose2dKey)
        }

        if (pose3dKey.isNotBlank()) {
            val pose2dFor3d = Pose2d(xMeters, yMeters, Rotation2d(headingRad))
            val pose3d = Pose3d(pose2dFor3d)
            table.put(pose3dKey, pose3d)
            removeLegacyPose3d(table, pose3dKey)
        }
    }

    override fun fromLog(table: LogTable) {
        // Optional: this is primarily used for recording, not replay.
    }

    private fun removeLegacyPose2d(table: LogTable, key: String) {
        table.remove("$key/translation/x")
        table.remove("$key/translation/y")
        table.remove("$key/rotation/value")

        table.remove("$key/x")
        table.remove("$key/y")
        table.remove("$key/theta")
    }

    private fun removeLegacyPose3d(table: LogTable, key: String) {
        table.remove("$key/translation/x")
        table.remove("$key/translation/y")
        table.remove("$key/translation/z")

        table.remove("$key/rotation/q/w")
        table.remove("$key/rotation/q/x")
        table.remove("$key/rotation/q/y")
        table.remove("$key/rotation/q/z")

        table.remove("$key/rotation/qw")
        table.remove("$key/rotation/qx")
        table.remove("$key/rotation/qy")
        table.remove("$key/rotation/qz")
    }
}
