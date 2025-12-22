package org.psilynx.psikit.ftc

import org.psilynx.psikit.core.Logger

abstract class PsiKitOpMode: PsiKitLinearOpMode() {
    final override fun runOpMode() {
        psiKitSetup()
        psiKit_init()
        Logger.start()
        while(!psiKitIsStarted){
            psiKit_init_loop()
        }
        psiKit_start()
        while(!psiKitIsStopRequested){
            Logger.periodicBeforeUser()
            processHardwareInputs()

            psiKit_loop()

            Logger.periodicAfterUser(0.0, 0.0)
        }
        psiKit_stop()
        Logger.end()

    }

    abstract fun psiKit_init()
    abstract fun psiKit_init_loop()
    abstract fun psiKit_start()
    abstract fun psiKit_loop()
    abstract fun psiKit_stop()

}