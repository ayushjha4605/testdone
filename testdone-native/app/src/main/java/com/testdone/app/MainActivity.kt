package com.testdone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.testdone.app.di.AppContainer
import com.testdone.app.ui.TestDoneAppRoot
import com.testdone.app.ui.theme.TestDoneTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * v2.3.16 — MainActivity now implements Razorpay's PaymentResultListener.
 *
 * The Checkout SDK is Activity-based: it hands success/error to whichever
 * Activity opened the checkout. We simply re-post the result onto
 * [AppContainer.paymentResults] (a SharedFlow) — PlanScreen collects it while
 * a checkout is in flight, so the payment UI stays fully Compose-side.
 */
class MainActivity : ComponentActivity(), com.razorpay.PaymentResultListener {

    lateinit var container: AppContainer
    private val keepSplash = MutableStateFlow(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplash.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as TestDoneApp).container

        setContent {
            TestDoneTheme {
                TestDoneAppRoot(container = container)
            }
        }

        // Hold the native splash just long enough for the in-app cinematic splash
        lifecycleScope.launch {
            delay(350)
            keepSplash.value = false
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        container.paymentResults.tryEmit(
            com.testdone.app.di.PaymentOutcome.Success(razorpayPaymentId ?: "unknown")
        )
    }

    override fun onPaymentError(code: Int, message: String?) {
        container.paymentResults.tryEmit(
            com.testdone.app.di.PaymentOutcome.Error(code, message ?: "unknown error")
        )
    }
}
