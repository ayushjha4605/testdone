package com.testdone.app.ui.password

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.testdone.app.data.repository.FirebaseAuthRepository
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdButton
import com.testdone.app.ui.components.TdButtonStyle
import com.testdone.app.ui.theme.TdExt
import kotlinx.coroutines.launch

/** Change password (logged-in users, via Firebase Auth). */
@Composable
fun ChangePasswordScreen(navController: NavHostController, vm: AppViewModel) {
    var current by rememberSaveable { mutableStateOf("") }
    var newPass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding() // OUTSIDE the scroll — viewport shrinks above the keyboard
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("Change Password", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("Keep your account secure", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (done) {
            Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TdExt.colors.success, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Password updated!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Use your new password next time you log in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    TdButton("Done", onClick = { navController.popBackStack() }, style = TdButtonStyle.SECONDARY)
                }
            }
            return@Column
        }

        PwdField("Current password", current, { current = it })
        Spacer(Modifier.height(12.dp))
        PwdField("New password (6+ chars)", newPass, { newPass = it })
        Spacer(Modifier.height(12.dp))
        PwdField("Confirm new password", confirm, { confirm = it })

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = TdExt.colors.danger)
        }

        Spacer(Modifier.height(24.dp))
        TdButton(
            text = "Update password",
            loading = loading,
            onClick = {
                when {
                    vm.container.sessionStore.isLoggedIn.not() -> error = "Pehle login karo password badalne ke liye"
                    newPass.length < 6 -> error = "New password kam se kam 6 characters ka ho"
                    newPass != confirm -> error = "Naye passwords match nahi kar rahe"
                    else -> {
                        scope.launch {
                            loading = true
                            error = null
                            val result = vm.container.authRepository.changePassword(newPass, current.ifBlank { null })
                            loading = false
                            result
                                .onSuccess {
                                    vm.container.sessionCoordinator.toast("Password updated", null, com.testdone.app.di.SessionCoordinator.Toast.Kind.SUCCESS)
                                    done = true
                                }
                                .onFailure { e ->
                                    error = FirebaseAuthRepository.friendlyAuthError(null, e.message)
                                }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(130.dp)) // clearance for the bottom nav bar
    }
}

/** Forgot password — sends a Firebase reset email. */
@Composable
fun ForgotPasswordScreen(navController: NavHostController, vm: AppViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding() // OUTSIDE the scroll — viewport shrinks above the keyboard
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Forgot Password", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        if (sent) {
            Icon(Icons.Rounded.Email, contentDescription = null, tint = TdExt.colors.success, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(14.dp))
            Text("Check your inbox", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Reset link bhej diya gaya hai $email pe. Spam folder bhi check karo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(18.dp))
            TdButton("Done", onClick = { navController.popBackStack() }, style = TdButtonStyle.SECONDARY)
            return@Column
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("you@example.com") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = TdExt.colors.danger)
        }

        Spacer(Modifier.height(20.dp))
        TdButton(
            text = if (loading) "Bhej rahe hain…" else "Send reset link",
            loading = loading,
            onClick = {
                if (email.isBlank()) {
                    error = "Email daalo"
                    return@TdButton
                }
                scope.launch {
                    loading = true
                    error = null
                    vm.container.authRepository.sendRecovery(email)
                        .onSuccess { sent = true }
                        .onFailure { e ->
                            // v2.3.14 — exact release wording for send failure
                            error = e.message?.let { FirebaseAuthRepository.friendlyAuthError(null, it) } ?: "Email nahi bhej paye"
                        }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(130.dp)) // clearance for the bottom nav bar
    }
}

@Composable
private fun PwdField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
