package com.rajatnagpure.pcmplayerconverter.ui.components
import com.rajatnagpure.pcmplayerconverter.ui.components.AppText as Text

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatnagpure.pcmplayerconverter.R
import com.rajatnagpure.pcmplayerconverter.util.HapticsManager

/** Dismissable cross-promo card for the Color Shift game. Visibility/capping is owned by PromoViewModel. */
@Composable
fun PromoBanner(
    visible: Boolean,
    onPlayClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                // Accent outline so the card reads as a highlight, not another settings panel
                .border(1.dp, accent.copy(alpha = 0.45f), shape)
                .clip(shape)
                .clickable {
                    HapticsManager.perform(view)
                    onPlayClick()
                },
            cornerRadius = 20.dp,
            contentPadding = 12.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.promo_color_shift),
                    contentDescription = "Color Shift logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Free game",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(accent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try Color Shift",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent
                    )
                    Text(
                        text = "A puzzle game by the maker of this app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                AppButton(
                    text = "Play",
                    onClick = onPlayClick,
                    modifier = Modifier.wrapContentSize()
                )
                AppIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
