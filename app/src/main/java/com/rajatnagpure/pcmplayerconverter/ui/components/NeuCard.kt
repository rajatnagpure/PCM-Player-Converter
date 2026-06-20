package com.rajatnagpure.pcmplayerconverter.ui.components
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism.neumorphic

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.neumorphic(isPressed = false, cornerRadius = cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
