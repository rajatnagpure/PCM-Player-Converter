package com.rajatnagpure.pcmplayerconverter.ui.theme.neumorphism
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.rajatnagpure.pcmplayerconverter.ui.theme.LocalNeuTheme

fun Modifier.neumorphic(
    isPressed: Boolean = false,
    cornerRadius: Dp = 16.dp
): Modifier = composed {
    val neuTheme = LocalNeuTheme.current
    val lightColor = neuTheme.lightShadow.toArgb()
    val darkColor = neuTheme.darkShadow.toArgb()
    val bgColor = neuTheme.background.toArgb()

    this.drawBehind {
        val radius = cornerRadius.toPx()
        drawIntoCanvas { canvas ->
            val frameworkPaint = Paint().asFrameworkPaint()
            frameworkPaint.color = bgColor

            if (!isPressed) {
                // Light shadow (top-left)
                frameworkPaint.setShadowLayer(
                    15f,
                    -8f,
                    -8f,
                    lightColor
                )
                canvas.drawRoundRect(
                    0f, 0f, size.width, size.height,
                    radius, radius, Paint().apply { this.asFrameworkPaint().set(frameworkPaint) }
                )

                // Dark shadow (bottom-right)
                frameworkPaint.setShadowLayer(
                    15f,
                    8f,
                    8f,
                    darkColor
                )
                canvas.drawRoundRect(
                    0f, 0f, size.width, size.height,
                    radius, radius, Paint().apply { this.asFrameworkPaint().set(frameworkPaint) }
                )
            } else {
                // Pressed state - reduced or inner shadow simulation
                frameworkPaint.setShadowLayer(
                    5f,
                    2f,
                    2f,
                    darkColor
                )
                canvas.drawRoundRect(
                    0f, 0f, size.width, size.height,
                    radius, radius, Paint().apply { this.asFrameworkPaint().set(frameworkPaint) }
                )
            }
        }
    }
}
