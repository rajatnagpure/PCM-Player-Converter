package com.rajatnagpure.pcmplayerconverter.ui.components
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rajatnagpure.pcmplayerconverter.ui.theme.NeuTheme
import com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism.neumorphic
import com.rajatnagpure.pcmplayerconverter.util.HapticsManager

@Composable
fun ThemeSelectionDialog(
    currentTheme: NeuTheme,
    onThemeSelected: (NeuTheme) -> Unit,
    onDismiss: () -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    analyticsEnabled: Boolean,
    onAnalyticsChanged: (Boolean) -> Unit
) {
    val view = LocalView.current
    var hapticsEnabled by remember { mutableStateOf(HapticsManager.isEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // ── HEADER ────────────────────────────────────────────────
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ── THEME PICKER ──────────────────────────────────────────
                Text(
                    text = "THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                NeuTheme.values().forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(cornerRadius = 12.dp, isPressed = currentTheme == theme)
                            .clickable {
                                HapticsManager.perform(view)
                                onThemeSelected(theme)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = {
                                HapticsManager.perform(view)
                                onThemeSelected(theme)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = theme.title.uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(20.dp))

                // ── HAPTICS TOGGLE ────────────────────────────────────────
                Text(
                    text = "HAPTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SettingsToggleRow(
                    icon = Icons.Filled.Vibration,
                    label = "Haptics",
                    checked = hapticsEnabled,
                    onCheckedChange = { enabled ->
                        hapticsEnabled = enabled
                        onHapticsChanged(enabled)
                        if (enabled) view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── PRIVACY ───────────────────────────────────────────────
                Text(
                    text = "PRIVACY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SettingsToggleRow(
                    icon = Icons.Filled.Insights,
                    label = "Share anonymous usage & crash data",
                    checked = analyticsEnabled,
                    onCheckedChange = { enabled ->
                        HapticsManager.perform(view)
                        onAnalyticsChanged(enabled)
                    }
                )
            }
        }
    }
}


@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(cornerRadius = 12.dp, isPressed = false)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}
