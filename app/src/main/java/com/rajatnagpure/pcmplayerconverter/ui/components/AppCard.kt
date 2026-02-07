package com.rajatnagpure.pcmplayerconverter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientStart
import com.rajatnagpure.pcmplayerconverter.ui.theme.GradientEnd

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    reverseGradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val gradientAlpha = 0.12f
    val gradientColors = if (reverseGradient) {
        listOf(GradientEnd.copy(alpha = gradientAlpha), GradientStart.copy(alpha = gradientAlpha))
    } else {
        listOf(GradientStart.copy(alpha = gradientAlpha), GradientEnd.copy(alpha = gradientAlpha))
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(modifier = Modifier.background(Brush.linearGradient(gradientColors))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}
