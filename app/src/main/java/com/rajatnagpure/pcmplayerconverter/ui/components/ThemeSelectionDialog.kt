package com.rajatnagpure.pcmplayerconverter.ui.components
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rajatnagpure.pcmplayerconverter.ui.theme.NeuTheme
import com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism.neumorphic

@Composable
fun ThemeSelectionDialog(
    currentTheme: NeuTheme,
    onThemeSelected: (NeuTheme) -> Unit,
    onDismiss: () -> Unit
) {
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
            val view = androidx.compose.ui.platform.LocalView.current
                Text(
                    text = "SELECT THEME",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NeuTheme.values().forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(cornerRadius = 12.dp, isPressed = currentTheme == theme)
                            .clickable { 
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                onThemeSelected(theme)
                                onDismiss() 
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { 
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
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
            }
        }
    }
}
