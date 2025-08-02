package org.firstinspires.ftc.teamcode.fakehardware

import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry

class FakeTelemetryItem(caption: String, value: Any? = null): Telemetry.Item {
    private var _caption = ""
    private var _value: Any? = null

    override fun getCaption() = _caption

    override fun setCaption(p0: String?): Telemetry.Item {
        _caption = p0!!
        return this
    }

    override fun setValue(p0: String?, vararg p1: Any?): Telemetry.Item {
        _caption = p0!!
        _value = p1
        return this
    }

    override fun setValue(p0: Any?): Telemetry.Item {
        _value = p0
        return this
    }

    override fun <T : Any?> setValue(p0: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> setValue(p0: String?, p1: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun setRetained(p0: Boolean?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun isRetained(): Boolean {
        TODO("Not yet implemented")
    }

    override fun addData(p0: String?, p1: String?, vararg p2: Any?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun addData(p0: String?, p1: Any?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> addData(p0: String?, p1: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> addData(p0: String?, p1: String?, p2: Func<T>?): Telemetry.Item {
        TODO("Not yet implemented")
    }
}