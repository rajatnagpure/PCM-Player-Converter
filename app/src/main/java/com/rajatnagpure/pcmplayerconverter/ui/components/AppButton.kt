package com.rajatnagpure.pcmplayerconverter.ui.components
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism.neumorphic

@Composable
fun AppButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overrideColor: androidx.compose.ui.graphics.Color? = null,
    iconColor: androidx.compose.ui.graphics.Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val contentColor = if (enabled) (overrideColor ?: MaterialTheme.colorScheme.primary) else Color.Gray.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .neumorphic(isPressed = isPressed, cornerRadius = 16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconColor ?: contentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
