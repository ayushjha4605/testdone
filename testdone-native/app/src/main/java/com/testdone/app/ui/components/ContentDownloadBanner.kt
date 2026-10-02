package com.testdone.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testdone.app.data.content.OtaSyncer
import com.testdone.app.ui.theme.TdExt

/**
 * v2.3.14 — content download banner shown while exam content is on its way.
 * Tap = retry the OTA sync. Auto-hides once content is fully synced.
 */
@Composable
fun ContentDownloadBanner(
    state: OtaSyncer.SyncUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        OtaSyncer.SyncUiState.Hidden -> Unit
        is OtaSyncer.SyncUiState.Downloading -> Banner(
            modifier = modifier,
            icon = { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = TdExt.colors.info) },
            text = "Exam content download ho raha hai — internet on rakhna. Ho gaya to ye banner hat jayega.",
            onClick = null,
        )
        OtaSyncer.SyncUiState.NeedsInternet -> Banner(
            modifier = modifier,
            icon = { Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = TdExt.colors.warning, modifier = Modifier.size(16.dp)) },
            text = "Exam content internet se aayega — internet on karo ya banner pe tap karke retry karo.",
            onClick = onRetry,
        )
        is OtaSyncer.SyncUiState.Failed -> Banner(
            modifier = modifier,
            icon = { Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = TdExt.colors.danger, modifier = Modifier.size(16.dp)) },
            text = "Content download fail hua — tap karke dobara try karo. (${state.detail})",
            onClick = onRetry,
        )
    }
}

@Composable
private fun Banner(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    text: String,
    onClick: (() -> Unit)?,

) {
    Row(
        modifier
            .animateContentSize()
            .then(
                if (onClick != null) Modifier.pressableScale(0.98f, onClick) else Modifier,
            )
            .fillMaxWidth()
            .background(TdExt.colors.info.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
