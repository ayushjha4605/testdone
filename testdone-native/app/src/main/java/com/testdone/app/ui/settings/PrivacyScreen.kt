package com.testdone.app.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.testdone.app.ui.AppViewModel
import com.testdone.app.ui.components.TdCard
import com.testdone.app.ui.components.pressableScale
import com.testdone.app.ui.theme.TdExt

/**
 * v2.3.14 — in-app privacy policy (recovered verbatim from the release APK),
 * replacing the toast-link shortcut of v2.3.8.
 */
@Composable
fun PrivacyScreen(navController: NavHostController, vm: AppViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                .padding(top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("Privacy Policy", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("How we handle data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        TdCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            Column {
                SectionTitle("1. What TestDone collects")

                PolicyBullet(
                    "Account basics only: your email/phone, display name, and exam preferences. " +
                        "No third-party trackers, no ad SDKs, no data brokers.",
                )
                PolicyBullet(
                    "Community content you post: doubts you share and questions you submit for review " +
                        "(with your display name, so you get credited when approved).",
                )
                PolicyBullet(
                    "Test attempts and scores — stored so your history and analytics survive across devices.",
                )

                Spacer(Modifier.height(14.dp))
                SectionTitle("2. What TestDone does NOT collect")

                PolicyBullet(
                    "No contacts, no photos, no location, no advertising identifiers, " +
                        "and nothing from other apps on your phone.",
                )

                Spacer(Modifier.height(14.dp))
                SectionTitle("3. Where your data lives")

                PolicyBullet(
                    "Your account and study data are stored in Google Firebase (Google Cloud, with data " +
                        "encrypted in transit and at rest) under TestDone's Firebase project.",
                )
                PolicyBullet(
                    "Downloaded exam content is cached on your device so the app works offline; " +
                        "clearing app storage removes it.",
                )

                Spacer(Modifier.height(14.dp))
                SectionTitle("4. Who can see what")

                PolicyBullet(
                    "Doubts and approved community questions are public inside the TestDone community, " +
                        "with the display name you set in your profile.",
                )
                PolicyBullet(
                    "TestDone's review team can see submitted questions (question text, options, your name) " +
                        "for moderation and quality review only.",
                )
                PolicyBullet(
                    "Applying for an exam or contacting support does not share any extra data beyond " +
                        "what you type in the message.",
                )

                Spacer(Modifier.height(14.dp))
                SectionTitle("5. Deleting your account")

                PolicyBullet(
                    "You can request full account deletion at testdoneadmin@gmail.com — your account, " +
                        "profile, and community posts are removed. Study history tied to your account " +
                        "is deleted too.",
                )

                Spacer(Modifier.height(14.dp))
                SectionTitle("6. Age & safety")

                PolicyBullet("TestDone is intended for exam aspirants aged 13 and above.")
                PolicyBullet(
                    "Suspicious or abusive community accounts can be suspended by moderation.",
                )

                Spacer(Modifier.height(14.dp))

                // v2.3.16 — the "no external website" note was removed
                // (user request); the mailto support row below remains the
                // only footer element.

                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressableScale(0.97f) {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_SENDTO,
                                        android.net.Uri.parse("mailto:testdoneadmin@gmail.com"),
                                    ),
                                )
                            }
                        }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        "Questions? Email testdoneadmin@gmail.com",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(130.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PolicyBullet(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium, color = TdExt.colors.info)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
