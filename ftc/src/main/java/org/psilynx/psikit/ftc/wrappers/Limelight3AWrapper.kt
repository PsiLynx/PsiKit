package org.psilynx.psikit.ftc.wrappers

import com.qualcomm.hardware.limelightvision.LLFieldMap
import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.LLResultTypes.CalibrationResult
import com.qualcomm.hardware.limelightvision.LLStatus
import com.qualcomm.hardware.limelightvision.Limelight3A
import com.qualcomm.hardware.limelightvision.PsiKitLimelightJsonFactory
import com.qualcomm.robotcore.hardware.HardwareDevice
import org.firstinspires.ftc.robotcore.internal.usb.EthernetOverUsbSerialNumber
import org.json.JSONObject
import org.psilynx.psikit.ftc.FtcLogTuning
import org.psilynx.psikit.core.LogTable
import org.psilynx.psikit.core.Logger
import org.psilynx.psikit.core.wpi.math.Pose2d
import org.psilynx.psikit.core.wpi.math.Pose3d
import org.psilynx.psikit.core.wpi.math.Rotation2d
import java.lang.reflect.Field
import java.net.InetAddress

class Limelight3AWrapper(
    private val device: Limelight3A?
) : Limelight3A(
    EthernetOverUsbSerialNumber.fromIpAddress("127.0.0.1", "psikit"),
    device?.deviceName ?: "MockLimelight3A",
    InetAddress.getLoopbackAddress()
), HardwareInput<Limelight3A> {

    private var cachedResultJson: String = ""
    private var cachedResultTimestampMs: Long = 0L
    private var cachedStatusJson: String = ""

    private var cachedRunning: Boolean = false
    private var cachedConnected: Boolean = false

    private var cachedManufacturer: HardwareDevice.Manufacturer = HardwareDevice.Manufacturer.Other
    private var cachedDeviceName: String = device?.deviceName ?: "MockLimelight3A"
    private var cachedConnectionInfo: String = ""
    private var cachedVersion: Int = 1

    @Volatile private var replayResult: LLResult? = null
    @Volatile private var replayStatus: LLStatus? = null

    override fun new(wrapped: Limelight3A?) = Limelight3AWrapper(wrapped)

    private fun putPose2dAnd3dFromJsonPose6(table: LogTable, keyPrefix: String, arr: org.json.JSONArray?) {
        if (arr == null || arr.length() < 6) return
        val x = arr.optDouble(0, 0.0)
        val y = arr.optDouble(1, 0.0)
        val yawDeg = arr.optDouble(5, 0.0)
        val pose2d = Pose2d(x, y, Rotation2d.fromDegrees(yawDeg))
        table.put("${keyPrefix}Pose2d", pose2d)
        table.put("${keyPrefix}Pose3d", Pose3d(pose2d))
    }

    private fun putDerivedResultFields(table: LogTable, resultJson: String) {
        val t = table.getSubtable("result")
        val field = table.getSubtable("field")

        if (resultJson.isBlank()) {
            t.put("valid", false)
            t.put("pipelineIndex", 0)
            t.put("tx", 0.0)
            t.put("ty", 0.0)
            t.put("fiducialCount", 0)
            t.put("fiducialId0", -1)

            // Field-friendly poses (structs)
            field.put("botPose2d", Pose2d.kZero)
            field.put("botPose3d", Pose3d.kZero)
            field.put("wpiBluePose2d", Pose2d.kZero)
            field.put("wpiBluePose3d", Pose3d.kZero)
            field.put("wpiRedPose2d", Pose2d.kZero)
            field.put("wpiRedPose3d", Pose3d.kZero)
            return
        }

        try {
            val obj = JSONObject(resultJson)
            val v = obj.optInt("v", 0)
            t.put("valid", v != 0)

            t.put("pipelineIndex", obj.optInt("pID", 0))
            t.put("tx", obj.optDouble("tx", 0.0))
            t.put("ty", obj.optDouble("ty", 0.0))

            val fid = obj.optJSONArray("Fiducial")
            val fidCount = fid?.length() ?: 0
            t.put("fiducialCount", fidCount)
            val fid0 = if (fidCount > 0) fid?.optJSONObject(0) else null
            t.put("fiducialId0", fid0?.optInt("fID", -1) ?: -1)

            // AdvantageScope field widgets can consume Pose2d/Pose3d structs directly.
            putPose2dAnd3dFromJsonPose6(field, "bot", obj.optJSONArray("botpose"))
            putPose2dAnd3dFromJsonPose6(field, "wpiBlue", obj.optJSONArray("botpose_wpiblue"))
            putPose2dAnd3dFromJsonPose6(field, "wpiRed", obj.optJSONArray("botpose_wpired"))
        } catch (_: Throwable) {
            // Keep logging resilient; if parsing fails we still have raw resultJson.
        }
    }

    override fun toLog(table: LogTable) {
        if (FtcLogTuning.bulkOnlyLogging) {
            return
        }

        // Avoid extra network calls here. The overrides for getLatestResult/getStatus will
        // opportunistically refresh caches when user code calls them.
        if (device != null) {
            try {
                cachedRunning = device.isRunning
            } catch (_: Throwable) {}
            try {
                cachedConnected = device.isConnected
            } catch (_: Throwable) {}
            try {
                captureResultJson(device.latestResult)
            } catch (_: Throwable) {}
            try {
                cachedManufacturer = device.manufacturer
                cachedDeviceName = device.deviceName
                cachedConnectionInfo = device.connectionInfo
                cachedVersion = device.version
            } catch (_: Throwable) {}
        }

        table.put("running", cachedRunning)
        table.put("connected", cachedConnected)
        table.put("resultJson", cachedResultJson)
        table.put("resultTimestampMs", cachedResultTimestampMs)
        table.put("statusJson", cachedStatusJson)

        putDerivedResultFields(table, cachedResultJson)

        table.put("deviceName", cachedDeviceName)
        table.put("connectionInfo", cachedConnectionInfo)
        table.put("version", cachedVersion)
        table.put("manufacturer", cachedManufacturer)
    }

    override fun fromLog(table: LogTable) {
        cachedRunning = table.get("running", false)
        cachedConnected = table.get("connected", false)
        cachedResultJson = table.get("resultJson", "")
        cachedResultTimestampMs = table.get("resultTimestampMs", 0L)
        cachedStatusJson = table.get("statusJson", "")

        cachedDeviceName = table.get("deviceName", "MockLimelight3A")
        cachedConnectionInfo = table.get("connectionInfo", "")
        cachedVersion = table.get("version", 1)
        cachedManufacturer = table.get("manufacturer", HardwareDevice.Manufacturer.Other)

        replayResult = PsiKitLimelightJsonFactory.resultFromJson(cachedResultJson, cachedResultTimestampMs)
        replayStatus = PsiKitLimelightJsonFactory.statusFromJson(cachedStatusJson)
    }

    private fun captureResultJson(result: LLResult?) {
        if (result == null) {
            cachedResultJson = ""
            cachedResultTimestampMs = 0L
            return
        }
        cachedResultTimestampMs = try {
            result.controlHubTimeStamp
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }

        // LLResult doesn't expose the raw JSON; pull it via reflection so replay can reconstruct
        // full-fidelity result objects.
        cachedResultJson = try {
            val f: Field = LLResult::class.java.getDeclaredField("jsonData")
            f.isAccessible = true
            val obj = f.get(result)
            (obj as? JSONObject)?.toString() ?: ""
        } catch (_: Throwable) {
            // Fallback to minimal JSON for common getters.
            try {
                JSONObject()
                    .put("tx", result.tx)
                    .put("ty", result.ty)
                    .put("ta", result.ta)
                    .put("ts", result.timestamp)
                    .put("v", if (result.isValid) 1 else 0)
                    .put("pID", result.pipelineIndex)
                    .put("pipelineType", result.pipelineType)
                    .toString()
            } catch (_: Throwable) {
                ""
            }
        }
    }

    private fun captureStatusJson(status: LLStatus?) {
        if (status == null) {
            cachedStatusJson = ""
            return
        }
        cachedStatusJson = try {
            val q = status.cameraQuat
            JSONObject()
                .put(
                    "cameraQuat",
                    JSONObject()
                        .put("w", q.w)
                        .put("x", q.x)
                        .put("y", q.y)
                        .put("z", q.z)
                )
                .put("cid", status.cid)
                .put("cpu", status.cpu)
                .put("finalYaw", status.finalYaw)
                .put("fps", status.fps)
                .put("hwType", status.hwType)
                .put("name", status.name)
                .put("pipeImgCount", status.pipeImgCount)
                .put("pipelineIndex", status.pipelineIndex)
                .put("pipelineType", status.pipelineType)
                .put("ram", status.ram)
                .put("snapshotMode", status.snapshotMode)
                .put("temp", status.temp)
                .toString()
        } catch (_: Throwable) {
            ""
        }
    }

    // --- Limelight API overrides ---

    override fun start() {
        if (Logger.isReplay()) return
        try {
            device?.start()
            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun pause() {
        if (Logger.isReplay()) return
        try {
            device?.pause()
            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun stop() {
        if (Logger.isReplay()) return
        try {
            device?.stop()
            cachedRunning = device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun isRunning(): Boolean {
        if (Logger.isReplay()) return cachedRunning
        return try {
            device?.isRunning ?: cachedRunning
        } catch (_: Throwable) {
            cachedRunning
        }
    }

    override fun setPollRateHz(rateHz: Int) {
        if (Logger.isReplay()) return
        try {
            device?.setPollRateHz(rateHz)
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun getTimeSinceLastUpdate(): Long {
        if (Logger.isReplay()) return 0L
        return try {
            device?.timeSinceLastUpdate ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    override fun isConnected(): Boolean {
        if (Logger.isReplay()) return cachedConnected
        return try {
            device?.isConnected ?: cachedConnected
        } catch (_: Throwable) {
            cachedConnected
        }
    }

    override fun getLatestResult(): LLResult? {
        if (Logger.isReplay() || device == null) {
            return replayResult
        }

        val r = try {
            device.latestResult
        } catch (_: Throwable) {
            null
        }
        captureResultJson(r)
        return r
    }

    override fun getStatus(): LLStatus {
        if (Logger.isReplay() || device == null) {
            return replayStatus ?: LLStatus()
        }

        val s = try {
            device.status
        } catch (_: Throwable) {
            null
        }
        captureStatusJson(s)
        return s ?: LLStatus()
    }

    override fun reloadPipeline(): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.reloadPipeline() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun pipelineSwitch(index: Int): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.pipelineSwitch(index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun captureSnapshot(snapname: String): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.captureSnapshot(snapname) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun deleteSnapshots(): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.deleteSnapshots() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun deleteSnapshot(snapname: String): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.deleteSnapshot(snapname) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updatePythonInputs(
        input1: Double,
        input2: Double,
        input3: Double,
        input4: Double,
        input5: Double,
        input6: Double,
        input7: Double,
        input8: Double
    ): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updatePythonInputs(input1, input2, input3, input4, input5, input6, input7, input8) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updatePythonInputs(inputs: DoubleArray): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updatePythonInputs(inputs) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun updateRobotOrientation(yaw: Double): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.updateRobotOrientation(yaw) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadPipeline(jsonString: String, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadPipeline(jsonString, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadFieldmap(fieldmap: LLFieldMap, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadFieldmap(fieldmap, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun uploadPython(pythonString: String, index: Int?): Boolean {
        if (Logger.isReplay()) return false
        return try {
            device?.uploadPython(pythonString, index) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun getCalDefault(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calDefault ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalFile(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calFile ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalEEPROM(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calEEPROM ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun getCalLatest(): CalibrationResult {
        if (Logger.isReplay()) return CalibrationResult()
        return try {
            device?.calLatest ?: CalibrationResult()
        } catch (_: Throwable) {
            CalibrationResult()
        }
    }

    override fun shutdown() {
        if (Logger.isReplay()) return
        try {
            device?.shutdown()
        } catch (_: Throwable) {
            // ignore
        }
    }

    // --- HardwareDevice overrides ---

    override fun getManufacturer(): HardwareDevice.Manufacturer {
        if (Logger.isReplay()) return cachedManufacturer
        return try {
            device?.manufacturer ?: cachedManufacturer
        } catch (_: Throwable) {
            cachedManufacturer
        }
    }

    override fun getDeviceName(): String {
        if (Logger.isReplay()) return cachedDeviceName
        return try {
            device?.deviceName ?: cachedDeviceName
        } catch (_: Throwable) {
            cachedDeviceName
        }
    }

    override fun getConnectionInfo(): String {
        if (Logger.isReplay()) return cachedConnectionInfo
        return try {
            device?.connectionInfo ?: cachedConnectionInfo
        } catch (_: Throwable) {
            cachedConnectionInfo
        }
    }

    override fun getVersion(): Int {
        if (Logger.isReplay()) return cachedVersion
        return try {
            device?.version ?: cachedVersion
        } catch (_: Throwable) {
            cachedVersion
        }
    }

    override fun resetDeviceConfigurationForOpMode() {
        if (Logger.isReplay()) return
        try {
            device?.resetDeviceConfigurationForOpMode()
        } catch (_: Throwable) {
            // ignore
        }
    }

    override fun close() {
        if (Logger.isReplay()) return
        try {
            device?.close()
        } catch (_: Throwable) {
            // ignore
        }
    }
}
