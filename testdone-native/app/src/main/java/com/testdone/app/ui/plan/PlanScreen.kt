package com.testdone.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.testdone.app.di.PaymentOutcome
import com.testdone.app.domain.model.Plan
import com.testdone.app.domain.model.PlanId
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.components.cssGradient
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp

/** Pricing page for TestDone Pass / Ultra. */
@Composable
fun PlanScreen(navController: NavHostController, vm: AppViewModel, planId: String) {
    val user by vm.user.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingPlan by remember { mutableStateOf<Plan?>(null) }
    val toast = vm.container.sessionCoordinator

    // v2.3.16 — payments config: OFF until the admin flips app_config/payments.
    val payments by produceState(initialValue = com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig()) {
        value = vm.container.paymentsRepository.config().getOrDefault(com.testdone.app.data.repository.PaymentsRepository.PaymentsConfig())
    }
    val paymentsReady = payments.ready

    // Razorpay results arrive via MainActivity → AppContainer.paymentResults
    LaunchedEffect(Unit) {
        vm.container.paymentResults.collect { outcome ->
            val plan = pendingPlan
            if (plan == null) return@collect
            when (outcome) {
                is PaymentOutcome.Success -> {
                    val session = vm.container.sessionStore.session.value
                    val expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    val wire = if (plan.id == "testdone-pass-ultra") "testdone-pass-ultra" else "testdone-pass"
                    // cloud first (plan follows the account), then local state
                    if (session != null) {
                        vm.container.profileRepository.activatePlan(session.userId, wire, expiresAt)
                    }
                    vm.container.settings.updateUser {
                        it.copy(
                            plan = if (plan.id == "testdone-pass-ultra") PlanId.ULTRA else PlanId.PASS,
                            planExpiry = expiresAt,
                        )
                    }
                    vm.container.paymentsRepository.recordPayment(plan.id, plan.price, outcome.razorpayPaymentId)
                    pendingPlan = null
                    busy = false
                    toast.toast(
                        "Payment successful!",
                        "${plan.name} activate ho gaya — enjoy!",
                        com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS,
                    )
                }
                is PaymentOutcome.Error -> {
                    pendingPlan = null
                    busy = false
                    toast.toast("Payment complete nahi hua", friendlyRazorpayError(outcome))
                }
            }
        }
    }

    /** Opens the Razorpay checkout sheet for [plan]. */
    fun launchCheckout(plan: Plan) {
        val keyId = payments.razorpayKeyId
        if (activity == null || keyId.isNullOrBlank()) {
            toast.toast("Payments ready nahi hain", "Thodi der baad try karo")
            return
        }
        pendingPlan = plan
        busy = true
        scope.launch {
            try {
                val co = com.razorpay.Checkout()
                co.setKeyID(keyId.trim())
                val prefill = org.json.JSONObject()
                user.email.takeIf { it.isNotBlank() }?.let { prefill.put("email", it) }
                user.phone.takeIf { it.isNotBlank() }?.let { prefill.put("contact", it) }
                user.name.takeIf { it.isNotBlank() }?.let { prefill.put("name", it) }
                val options = org.json.JSONObject()
                    .put("name", "TestDone")
                    .put("description", plan.name)
                    .put("currency", "INR")
                    .put("amount", (plan.price * 100).roundToInt())
                    .put("prefill", prefill)
                    .put("notes", org.json.JSONObject().put("planId", plan.id))
                    .put("theme", org.json.JSONObject().put("color", "#6366F1"))
                co.open(activity, options)
            } catch (e: Exception) {
                busy = false
                pendingPlan = null
                toast.toast("Payment start nahi hua", e.message ?: "Internet check karke dobara try karo")
            }
        }
    }

    val plans by vm.container.contentRepository.plans.collectAsState()
    val plansList = plans.ifEmpty {
        listOf(
            Plan("testdone-pass", "TestDone Pass", "Pass", 49.0, "month", "#6366F1", "", "Everything you need to ace your exam",
                listOf("All 162+ mock tests across 18 exams", "Full 18.6k+ real question bank", "80 previous year question sets", "Detailed solutions for every question", "Performance analytics & trends", "AI-powered insights", "Topper comparison & percentile", "Exam-realistic negative marking")),
            Plan("testdone-pass-ultra", "TestDone Pass Ultra", "Ultra", 59.0, "month", "#A78BFA", "", "Premium video lessons + everything in Pass",
                listOf("Everything in TestDone Pass", "Topic-wise HD video lessons (coming soon)", "24/7 AI doubt-solving tutor", "Live tournaments with toppers", "Premium handwritten notes", "Personalized study plan", "Priority doubt resolution", "Early access to new features"), popular = true),
        )
    }
    val currentPlan = plansList.firstOrNull { it.id == planId } ?: plansList.first()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Choose your plan", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        // plan cards
        plansList.forEach { plan ->
            val isCurrent = plan.id == currentPlan.id
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                    .border(
                        2.dp,
                        if (isCurrent) MaterialTheme.colorScheme.primary else TdExt.colors.cardBorder,
                        RoundedCornerShape(22.dp),
                    )
                    .padding(18.dp),
            ) {
                if (plan.popular) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .background(TdExt.colors.premiumBrush, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "MOST POPULAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color.White,
                        )
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .background(cssGradient(plan.gradientCss, listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(plan.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(plan.tagline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "₹${plan.price.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "/${plan.period}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    plan.features.forEach { f ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = TdExt.colors.success, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                f,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    if (isCurrent) {
                        if (paymentsReady) {
                            // v2.3.16 — Razorpay checkout (only when the admin
                            // enabled payments AND set the key id)
                            TdButton(
                                text = "Subscribe — ₹${plan.price.toInt()}/${plan.period}",
                                onClick = { launchCheckout(plan) },
                                loading = busy && pendingPlan?.id == plan.id,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    tint = TdExt.colors.warning,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.size(5.dp))
                                Text(
                                    "Secure payment · UPI, cards & netbanking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            TdButton(
                                text = "Subscribe — ₹${plan.price.toInt()}/${plan.period}",
                                onClick = {
                                    vm.container.sessionCoordinator.toast(
                                        "Payments launching soon",
                                        "Launch tak sab kuch FREE hai 🎉",
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "All tests are free until launch — no card needed.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        TdButton(
                            text = "Compare with ${plan.shortName}",
                            onClick = { navController.navigate(com.testdone.app.ui.nav.Routes.plan(plan.id)) },
                            style = TdButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            if (paymentsReady)
                "Payments powered by Razorpay · plans activate instantly after payment."
            else
                "Payments go live on Play Store soon. Until then every mock test, PYQ set and question is 100% free.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 130.dp), // clearance for the bottom nav bar
        )
    }
}

/** Friendly Hinglish mapping of Razorpay checkout error codes. */
private fun friendlyRazorpayError(e: PaymentOutcome.Error): String = when (e.code) {
    0 -> "Checkout cancel ho gaya — jab chaho dobara try karo"
    1 -> "Network issue lagta hai — internet check karke dobara try karo"
    2 -> "Payment start nahi ho paya — thodi der baad try karo"
    else -> e.message.take(120).ifBlank { "Payment fail ho gaya — dobara try karo" }
}
