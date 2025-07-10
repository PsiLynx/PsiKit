package org.psilynx.psikit.ftc.fakehardware

import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry

class FakeTelemetry:Telemetry {

    private var lines = arrayListOf<String>()

    override fun addData(p0: String?, p1: Any?): Telemetry.Item {
        if(p0 == null || p1 == null) throw NullPointerException("please dont give a null")
        lines.add("$p0: $p1")
        return FakeTelemetryItem(p0, p1)
    }

    override fun addLine(p0: String?): Telemetry.Line {
        val item = FakeTelemetryLine()
        lines.add(p0!!)
        return item
    }

    override fun update() = true

    override fun addData(p0: String?, p1: String?, vararg p2: Any?) = addData(p0, p1 as Any)

    override fun <T : Any?> addData(p0: String?, p1: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> addData(p0: String?, p1: String?, p2: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun removeItem(p0: Telemetry.Item?): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun clearAll() {
        TODO("Not yet implemented")
    }

    override fun addAction(p0: Runnable?): Any {
        TODO("Not yet implemented")
    }

    override fun removeAction(p0: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun speak(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun speak(p0: String?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun addLine(): Telemetry.Line {
        TODO("Not yet implemented")
    }

    override fun removeLine(p0: Telemetry.Line?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isAutoClear(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setAutoClear(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getMsTransmissionInterval(): Int {
        TODO("Not yet implemented")
    }

    override fun setMsTransmissionInterval(p0: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemSeparator(): String {
        TODO("Not yet implemented")
    }

    override fun setItemSeparator(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun getCaptionValueSeparator(): String {
        TODO("Not yet implemented")
    }

    override fun setCaptionValueSeparator(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun setDisplayFormat(p0: Telemetry.DisplayFormat?) {
        TODO("Not yet implemented")
    }

    override fun log(): Telemetry.Log {
        TODO("Not yet implemented")
    }
}