package org.psilynx.psikit.ftc

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.psilynx.psikit.core.Logger

/**
 * Logs goBILDA Pinpoint pose in AdvantageScope-friendly schemas for 2D/3D visualizations.
 *
 * Produces:
 * - `/Odometry/<deviceName>` with `Pose2d` and `Pose3d` struct fields
 * - `/Odometry` alias when there's exactly one Pinpoint configured
 */
class PinpointOdometryLogger {

    private class NamedPinpoint(
        val name: String,
        val update: () -> Unit,
        val position: () -> Pose2D,
        val poses: StructPoseInputs,
    ) {
        var lastSampleNs: Long = Long.MIN_VALUE
        var lastXMeters: Double = 0.0
        var lastYMeters: Double = 0.0
        var lastHeadingRad: Double = 0.0
    }

    private val cached = mutableListOf<NamedPinpoint>()
    private var cachedOnce = false

    private val robotAliases = StructPoseInputs("RobotPose", "RobotPose3d")

    private fun secondsSince(ns: Long): Double {
        if (ns == Long.MIN_VALUE) return Double.POSITIVE_INFINITY
        return (System.nanoTime() - ns) / 1_000_000_000.0
    }

    private fun shouldSampleNow(device: NamedPinpoint): Boolean {
        val period = FtcLogTuning.pinpointReadPeriodSec
        if (period <= 0.0) return true
        return secondsSince(device.lastSampleNs) >= period
    }

    fun logAll(hardwareMap: HardwareMap) {
        if (!cachedOnce) {
            cacheDevices(hardwareMap)
            cachedOnce = true
        }

        for (device in cached) {
            if (shouldSampleNow(device)) {
                device.lastSampleNs = System.nanoTime()
                device.update()
                val pose = device.position()
                device.lastXMeters = pose.getX(DistanceUnit.METER)
                device.lastYMeters = pose.getY(DistanceUnit.METER)
                device.lastHeadingRad = pose.getHeading(AngleUnit.RADIANS)
            }

            device.poses.set(device.lastXMeters, device.lastYMeters, device.lastHeadingRad)
            Logger.processInputs("/Odometry/${device.name}", device.poses)

            if (cached.size == 1) {
                robotAliases.set(device.lastXMeters, device.lastYMeters, device.lastHeadingRad)
                Logger.processInputs("/Odometry", robotAliases)
            }
        }
    }

    private fun cacheDevices(hardwareMap: HardwareMap) {
        cached.clear()

        val devices = try {
            hardwareMap.getAll(GoBildaPinpointDriver::class.java)
        } catch (_: Throwable) {
            emptyList()
        }

        for (device in devices) {
            val name = firstNameOrFallback(hardwareMap, device, "pinpoint")
            cached.add(
                NamedPinpoint(
                    name,
                    update = if (FtcLogTuning.pinpointLoggerCallsUpdate) {
                        { device.update() }
                    } else {
                        { }
                    },
                    position = { device.position },
                    poses = StructPoseInputs("Pose2d", "Pose3d"),
                )
            )
        }

        cached.sortBy { it.name }
    }

    private fun firstNameOrFallback(
        hardwareMap: HardwareMap,
        device: HardwareDevice,
        fallback: String,
    ): String {
        return try {
            val names = hardwareMap.getNamesOf(device)
            if (!names.isNullOrEmpty()) names.first() else fallback
        } catch (_: Throwable) {
            fallback
        }
    }

}
