package org.psilynx.psikit.ftc.test

import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(AppUtil::class)
class ShadowAppUtil {
    @Implementation
    fun loadLibrary(name: String?) {
        // no-op
    }
}