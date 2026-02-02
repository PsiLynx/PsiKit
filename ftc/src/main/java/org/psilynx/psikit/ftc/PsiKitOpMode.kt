package org.psilynx.psikit.ftc

/**
 * Legacy PsiKit OpMode API kept for compatibility.
 *
 * Prefer subclassing [PsiKitIterativeOpMode] (iterative) or using
 * com.qualcomm.robotcore.eventloop.opmode.PsiKitLinearOpMode (linear).
 */
@Deprecated(
    message = "Use PsiKitIterativeOpMode or com.qualcomm.robotcore.eventloop.opmode.PsiKitLinearOpMode",
    replaceWith = ReplaceWith("PsiKitIterativeOpMode")
)
abstract class PsiKitOpMode : PsiKitIterativeOpMode() {

    /** Called once during startup before Logger.start() (use for receivers + metadata). */
    abstract fun psiKit_init()

    /** Called each init_loop after PsiKit logging. */
    abstract fun psiKit_init_loop()

    /** Called exactly once when the OpMode transitions to started. */
    abstract fun psiKit_start()

    /** Called each loop after PsiKit logging. */
    abstract fun psiKit_loop()

    /** Called from stop before the PsiKit session is ended. */
    abstract fun psiKit_stop()

    final override fun onPsiKitConfigureLogging() {
        psiKit_init()
    }

    final override fun onPsiKitInitLoop() {
        psiKit_init_loop()
    }

    final override fun onPsiKitStart() {
        psiKit_start()
    }

    final override fun onPsiKitLoop() {
        psiKit_loop()
    }

    final override fun onPsiKitStop() {
        psiKit_stop()
    }
}