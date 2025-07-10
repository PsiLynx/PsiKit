package org.psilynx.psikit.ftc.fakehardware

class FakeTimer() {
    private var resetTime = 0.0
    fun restart() {
        resetTime = Companion.time
    }

    fun getDeltaTime() = Companion.time - resetTime

    fun waitUntil(time: Double) =
        if(time > getDeltaTime()) addTime(time - getDeltaTime())
        else {}

    companion object {
        fun addTime(time: Double) {
            this.time += time
        }
        var time = 0.0
    }
}