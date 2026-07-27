package com.yusufteker.planora.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.planora.core.data.dto.AppVersionConfigDto
import com.yusufteker.planora.shared.getPlatformName
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_later
import planora.core.generated.resources.action_update_now
import planora.core.generated.resources.force_update_message
import planora.core.generated.resources.force_update_title
import planora.core.generated.resources.optional_update_message
import planora.core.generated.resources.optional_update_title

/**
 * Non-dismissible (or optional) update modal dialog shown when a new application version is required or available.
 *
 * @param config Remote [AppVersionConfigDto] containing title, message, and store URLs.
 * @param isForceUpdate `true` if mandatory update blocks app usage, `false` for optional dismissible update.
 * @param onDismiss Callback invoked when optional update dialog is dismissed by user.
 */
@Composable
fun ForceUpdateDialog(
    config: AppVersionConfigDto,
    isForceUpdate: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    val platformName = getPlatformName()
    val isIos = platformName.contains("iOS", ignoreCase = true)
    val targetStoreUrl = if (isIos) config.storeUrlIos else config.storeUrlAndroid

    val defaultTitleRes = if (isForceUpdate) Res.string.force_update_title else Res.string.optional_update_title
    val defaultMsgRes = if (isForceUpdate) Res.string.force_update_message else Res.string.optional_update_message

    val titleText = if (isIos) {
        config.forceUpdateTitleEn ?: stringResource(defaultTitleRes)
    } else {
        config.forceUpdateTitleTr ?: stringResource(defaultTitleRes)
    }

    val messageText = if (isIos) {
        config.forceUpdateMessageEn ?: stringResource(defaultMsgRes)
    } else {
        config.forceUpdateMessageTr ?: stringResource(defaultMsgRes)
    }

    Dialog(
        onDismissRequest = {
            if (!isForceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (targetStoreUrl.isNotBlank()) {
                            uriHandler.openUri(targetStoreUrl)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(Res.string.action_update_now))
                }

                if (!isForceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(Res.string.action_later))
                    }
                }
            }
        }
    }
}
