package org.psilynx.psikit.ftc.test.fakehardware


class FakeTimer() {
    private var resetTime = 0.0
    fun restart() {
        lastLoop = getDeltaTime()
        resetTime = time
    }

    fun getDeltaTime() = time - resetTime

    fun waitUntil(time: Double) =
        if(time > getDeltaTime()) addTime(time - getDeltaTime())
        else {}

    companion object {
        var lastLoop = 0.0
        private set


        fun addTime(time: Double) {
            this.time += time
        }
        var time = 0.0
    }
}