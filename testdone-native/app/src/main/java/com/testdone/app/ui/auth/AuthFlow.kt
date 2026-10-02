package com.testdone.app.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testdone.app.R
import com.testdone.app.data.repository.AuthResult
import com.testdone.app.data.repository.PhoneFormat
import com.testdone.app.data.repository.PhoneLoginMode
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.components.ExamLogoBadge
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.theme.InterDisplayFamily
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Login / signup (Google · phone OTP · email) + onboarding exam picker. */
@Composable
fun AuthFlow(vm: AppViewModel) {
    var stage by rememberSaveable { mutableStateOf("login") } // login | signup | phone | onboard
    val user by vm.user.collectAsState()
    val phoneState by vm.phoneState.collectAsState()
    val activity = LocalContext.current as Activity

    // Logged in (Google / phone / email) but onboarding pending → go straight to
    // the exam picker. Watches the user state so it also fires right after login.
    LaunchedEffect(Unit) {
        vm.container.settings.user.collect { u ->
            if (vm.container.sessionStore.isLoggedIn && !u.onboarded && stage != "onboard") {
                stage = "onboard"
            }
        }
    }

    // v2.3.15 — instant flip: the view-model sets this the MOMENT a login
    // succeeds for a not-yet-onboarded account (no waiting for the cloud
    // hydrate chain that made login look dead on slow connections).
    val needsOnboarding by vm.authNeedsOnboarding.collectAsState()
    LaunchedEffect(needsOnboarding) {
        if (needsOnboarding && stage != "onboard") stage = "onboard"
    }

    // OTP screens are driven by the phone-flow state (not the stage history)
    val shown = if (phoneState.step == AppViewModel.PhoneAuthState.Step.OTP ||
        phoneState.step == AppViewModel.PhoneAuthState.Step.VERIFYING
    ) "otp" else stage

    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            val order = listOf("login", "signup", "phone", "otp", "onboard")
            val forward = order.indexOf(targetState) >= order.indexOf(initialState)
            val slide = if (forward) 1 else -1
            (slideInHorizontally(tween(300)) { it / 3 * slide } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(240)) { -it / 3 * slide } + fadeOut(tween(240)))
        },
        label = "authStage",
    ) { s ->
        when (s) {
            "otp" -> OtpScreen(vm = vm, onBack = { vm.resetPhoneFlow() })
            "phone" -> PhoneScreen(vm = vm, onBack = { vm.resetPhoneFlow(); stage = "login" })
            "onboard" -> OnboardingScreen(vm = vm, onBack = { stage = "login" })
            else -> LoginScreen(
                vm = vm,
                signupMode = s == "signup",
                onToggleMode = { stage = if (s == "signup") "login" else "signup" },
                onPhone = { stage = "phone" },
                onContinueDemo = { stage = "onboard" },
            )
        }
    }
}

// ── login / signup ──────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(
    vm: AppViewModel,
    signupMode: Boolean,
    onToggleMode: () -> Unit,
    onPhone: () -> Unit,
    onContinueDemo: () -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val activity = context as Activity
    val googleBusy by vm.googleBusy.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        if (email.isBlank() || password.length < 6) {
            error = "Email + 6+ char password daalo"
            return
        }
        scope.launch {
            loading = true
            error = null
            val result = if (signupMode) {
                vm.container.authRepository.signUp(email, password, name.ifBlank { null })
            } else {
                vm.container.authRepository.login(email, password)
            }
            loading = false
            when (result) {
                is AuthResult.Success -> {
                    // v2.3.15 — navigate/toast instantly; cloud hydrate runs in
                    // the background (the old inline await made the button look
                    // dead while profile/attempts/content synced over the net).
                    vm.onLoginSuccess(
                        result.session,
                        result.isNewUser,
                        if (signupMode) "Account created!" else "Welcome back!",
                    )
                    if (signupMode) {
                        // v2.3.14 — email verification nudge (exact release wording)
                        vm.container.sessionCoordinator.toast(
                            "Verify email",
                            "Inbox (aur spam folder) check karo — link verify karne ke baad account officially confirmed ho jata hai",
                            kind = com.testdone.app.di.SessionCoordinator.Toast.Kind.INFO,
                        )
                    }
                }
                is AuthResult.Error -> error = result.friendly
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding() // OUTSIDE the scroll — viewport shrinks above the keyboard
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        // logo + wordmark (v2.3.6 "Rising Check" vector lockup)
        Image(
            painter = painterResource(R.drawable.logo_full),
            contentDescription = null,
            modifier = Modifier.size(76.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            if (signupMode) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (signupMode) "18 exams · 18,600+ questions · free mock tests"
            else "Login to sync your progress across devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        // ── Google (one-tap, native) ─────────────────────────────────────────
        SocialButton(
            text = if (googleBusy.launching) "Signing in…" else "Continue with Google",
            icon = painterResource(R.drawable.ic_google_g),
            loading = googleBusy.launching,
        ) { vm.signInGoogle(activity) }

        Spacer(Modifier.height(14.dp))

        // ── Phone OTP ─────────────────────────────────────────────────────────
        SocialButton(
            text = "Continue with phone",
            icon = null,
            trailingIcon = { Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        ) { onPhone() }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text("  or email  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Spacer(Modifier.height(18.dp))

        // email form
        if (signupMode) {
            AuthField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = "Your name",
                icon = Icons.Rounded.Person,
            )
            Spacer(Modifier.height(12.dp))
        }
        AuthField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            placeholder = "you@example.com",
            icon = Icons.Rounded.Email,
            keyboard = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        AuthField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "At least 6 characters",
            icon = Icons.Rounded.Lock,
            keyboard = KeyboardType.Password,
            password = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword },
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                error!!,
                style = MaterialTheme.typography.bodySmall,
                color = TdExt.colors.danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(22.dp))
        TdButton(
            text = if (signupMode) "Create account" else "Login",
            onClick = { submit() },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.pressableScale(0.96f, onToggleMode),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (signupMode) "Already have an account? " else "New here? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (signupMode) "Login" else "Create account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Forgot password?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.pressableScale(0.96f) {
                vm.container.sessionCoordinator.toast(
                    "Forgot password",
                    "Login screen se email daalo — ya Settings → Forgot Password",
                    com.testdone.app.di.SessionCoordinator.Toast.Kind.INFO,
                )
            },
        )

        Spacer(Modifier.height(26.dp))
        Text(
            "Continue without account",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .pressableScale(0.94f, onContinueDemo)
                .padding(8.dp),
        )
        Spacer(Modifier.height(30.dp))
    }
}

/** Google / phone style full-width outline buttons (official sign-in look). */
@Composable
private fun SocialButton(
    text: String,
    icon: androidx.compose.ui.graphics.painter.Painter?,
    loading: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressableScale(0.97f, onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            if (icon != null) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            if (trailingIcon != null) trailingIcon()
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── phone login (v2.3.8: free password mode · SMS OTP via Remote Config) ────

/**
 * Phone login screen — dual-mode (v2.3.8):
 *  • PASSWORD (default, free on Spark): number + password, zero SMS.
 *  • SMS (Remote Config "phone_login_mode" = "sms"): classic Firebase OTP.
 *    Password login stays reachable as a secondary link for old accounts.
 */
@Composable
private fun PhoneScreen(vm: AppViewModel, onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    val state by vm.phoneState.collectAsState()
    val mode by vm.phoneLoginMode.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var raw by rememberSaveable { mutableStateOf("") }

    // password sub-flow state (free login)
    var usePassword by rememberSaveable(mode) { mutableStateOf(mode == PhoneLoginMode.PASSWORD) }
    var signup by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var pwError by rememberSaveable { mutableStateOf<String?>(null) }

    fun submitPassword() {
        val e164 = PhoneFormat.toE164(raw)
        if (e164 == null) {
            pwError = "Sahi 10-digit mobile number daalo (6-9 se start)"
            return
        }
        if (password.length < 6) {
            pwError = "Password kam se kam 6 characters ka rakho"
            return
        }
        scope.launch {
            loading = true
            pwError = null
            val result = if (signup) {
                vm.container.authRepository.signUpWithPhonePassword(e164, password)
            } else {
                vm.container.authRepository.loginWithPhonePassword(e164, password)
            }
            loading = false
            when (result) {
                is AuthResult.Success -> {
                    // v2.3.15 — instant navigation; background cloud hydrate
                    // (previously the inline onLogin() await froze this screen
                    // through the whole profile/attempts/content sync).
                    vm.onLoginSuccess(
                        result.session,
                        result.isNewUser,
                        if (signup) "Account created!" else "Welcome back!",
                    )
                }
                is AuthResult.Error -> pwError = result.friendly
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text("Login with phone", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (usePassword) "Number + password — OTP-free, free login"
                    else "OTP se verify hoga — data bhi save",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(36.dp))

        Text(
            "PHONE NUMBER".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it.filter { c -> c.isDigit() }.take(10) },
            prefix = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "+91",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .width(1.dp)
                            .height(22.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            },
            placeholder = { Text("98765 43210", style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (usePassword) {
            // ── free login: number + password (v2.3.8) ─────────────────────
            Spacer(Modifier.height(14.dp))
            AuthField(
                value = password,
                onValueChange = { password = it },
                label = if (signup) "Set a password" else "Password",
                placeholder = "At least 6 characters",
                icon = Icons.Rounded.Lock,
                keyboard = KeyboardType.Password,
                password = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
            )

            if (pwError != null) {
                Spacer(Modifier.height(10.dp))
                Text(pwError!!, style = MaterialTheme.typography.bodySmall, color = TdExt.colors.danger)
            }

            Spacer(Modifier.height(22.dp))
            TdButton(
                text = if (loading) "Please wait…" else if (signup) "Create account" else "Login",
                onClick = { submitPassword() },
                enabled = raw.length == 10 && !loading,
                loading = loading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.pressableScale(0.96f) { signup = !signup },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (signup) "Pehle se account hai? " else "Naya number hai? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (signup) "Login" else "Create account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Forgot password?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.pressableScale(0.96f) {
                    vm.container.sessionCoordinator.toast(
                        "Forgot password",
                        "Password reset email nahi jaata is login me. SMS OTP on hote hi OTP se verify karke Settings → Change Password use karo — ya abhi naya account bana lo.",
                        com.testdone.app.di.SessionCoordinator.Toast.Kind.INFO,
                    )
                },
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "No SMS, no OTP — number hi tumhara username hai. Google/Email se bhi login kar sakte ho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // SMS available (Blaze on) → offer the OTP path too
            if (mode == PhoneLoginMode.SMS) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "SMS OTP se login",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .pressableScale(0.94f) { usePassword = false }
                        .padding(8.dp),
                )
            }
        } else {
            // ── SMS OTP path (classic Firebase Phone Auth) ─────────────────
            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(state.error!!, style = MaterialTheme.typography.bodySmall, color = TdExt.colors.danger)
            }

            Spacer(Modifier.height(24.dp))
            TdButton(
                text = if (state.step == AppViewModel.PhoneAuthState.Step.SENDING) "Sending OTP…" else "Send OTP",
                onClick = {
                    PhoneFormat.toE164(raw)?.let { vm.startPhoneLogin(activity, it) }
                },
                enabled = raw.length == 10,
                loading = state.step == AppViewModel.PhoneAuthState.Step.SENDING,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "6-digit code SMS pe aayega. Google/Email se bhi login kar sakte ho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // password accounts can still log in when SMS is the default
            Spacer(Modifier.height(8.dp))
            Text(
                "Password se login (free)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressableScale(0.94f) { usePassword = true }
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}

// ── OTP verification ─────────────────────────────────────────────────────────

@Composable
private fun OtpScreen(vm: AppViewModel, onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    val state by vm.phoneState.collectAsState()

    var otp by rememberSaveable { mutableStateOf("") }
    var resendIn by remember { mutableIntStateOf(60) }

    // 60s resend countdown, restarting whenever a new OTP is sent
    LaunchedEffect(state.phone, state.step) {
        if (state.step == AppViewModel.PhoneAuthState.Step.OTP) {
            resendIn = 60
            while (resendIn > 0) {
                delay(1000)
                resendIn--
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                Text("Verify your number", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "6-digit code bheja gaya hai",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            PhoneFormat.pretty(state.phone) + " pe",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(30.dp))

        OtpInput(
            value = otp,
            onValueChange = { new ->
                otp = new
                if (new.length == 6) vm.submitOtp(new) // auto-submit on the 6th digit
            },
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, style = MaterialTheme.typography.bodySmall, color = TdExt.colors.danger, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(28.dp))
        TdButton(
            text = if (state.step == AppViewModel.PhoneAuthState.Step.VERIFYING) "Verifying…" else "Verify & Login",
            onClick = { vm.submitOtp(otp) },
            enabled = otp.length == 6,
            loading = state.step == AppViewModel.PhoneAuthState.Step.VERIFYING,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        if (resendIn > 0) {
            Text(
                "Resend OTP in ${resendIn}s",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Resend OTP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressableScale(0.95f) { vm.resendOtp(activity) }
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}

/** Six-box OTP input with a hidden capture field (opens keyboard, digits only). */
@Composable
private fun OtpInput(value: String, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // invisible single capture field — keeps caret & digit filtering in one place
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) onValueChange(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .width(1.dp)
                .height(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
                .focusable(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) { i ->
                val ch = value.getOrNull(i)?.toString() ?: ""
                val active = i == value.length
                Box(
                    Modifier
                        .size(52.dp)
                        .clickable { focusRequester.requestFocus(); keyboard?.show() }
                        .background(
                            if (ch.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = if (active || ch.isNotEmpty()) 1.5.dp else 1.dp,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else if (ch.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (ch.isNotEmpty()) {
                        Text(
                            ch,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else if (active) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
                        )
                    }
                }
            }
        }
    }
}

// ── field ───────────────────────────────────────────────────────────────────

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (password && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else null,
            visualTransformation = if (password && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── onboarding (exam picker) ────────────────────────────────────────────────

@Composable
private fun OnboardingScreen(vm: AppViewModel, onBack: () -> Unit) {
    val exams by vm.container.contentRepository.exams.collectAsState()
    val categories by vm.container.contentRepository.categories.collectAsState()
    var selectedCat by rememberSaveable { mutableStateOf("all") }
    var selectedExam by rememberSaveable { mutableStateOf<String?>(null) }
    val user by vm.user.collectAsState()

    val filtered = if (selectedCat == "all") exams else exams.filter { it.category == selectedCat }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = vm.container.sessionStore.isLoggedIn.not()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Pick your target exam",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    // v2.3.14 — exact release wording
                    "Apna target exam choose karke preparation shuru karo. Email verify karna ho to Profile → \"Verify email\" dabao.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // category chips
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryChip("all", "All", selectedCat == "all") { selectedCat = "all" }
            }
            items(categories.size) { i ->
                val cat = categories[i]
                CategoryChip(cat.id, "${cat.icon} ${cat.label}", selectedCat == cat.id) { selectedCat = cat.id }
            }
        }

        Spacer(Modifier.height(12.dp))

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(filtered.size) { i ->
                val exam = filtered[i]
                val isSelected = selectedExam == exam.id
                Box(
                    Modifier
                        .pressableScale(0.95f) { selectedExam = exam.id }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(18.dp),
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else TdExt.colors.cardBorder,
                            RoundedCornerShape(18.dp),
                        )
                        .padding(14.dp),
                ) {
                    Column {
                        ExamLogoBadge(examId = exam.id, size = 46.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            exam.shortName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${exam.subjects.size} subjects",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        TdButton(
            text = if (selectedExam != null) "Start Preparing" else "Select an exam",
            onClick = {
                val examId = selectedExam ?: return@TdButton
                vm.container.settings.updateUser { it.copy(selectedExamId = examId, onboarded = true, name = it.name.ifBlank { user.email?.substringBefore("@") ?: "Topper" }) }
                vm.container.settings.setOnboarded(true)
                // persist to cloud when logged in
                vm.updateCloudProfile(
                    com.testdone.app.domain.model.ProfilePatch(selectedExamId = examId, onboarded = true),
                )
                vm.onAuthSuccess()
            },
            enabled = selectedExam != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CategoryChip(id: String, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .pressableScale(0.94f, onClick)
            .background(
                if (active) TdExt.colors.brandBrush
                else androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface),
                ),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
