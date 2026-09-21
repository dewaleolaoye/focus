package com.websiteblocker.app.ui

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.websiteblocker.app.R
import com.websiteblocker.app.domain.ServiceProfile

@Composable
fun ServiceIcon(profile: ServiceProfile, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bundledIcon =
        when (profile.id) {
            "instagram" -> R.drawable.brand_instagram
            "youtube" -> R.drawable.brand_youtube
            "whatsapp" -> R.drawable.brand_whatsapp
            "tiktok" -> R.drawable.brand_tiktok
            "x" -> R.drawable.brand_x
            else -> null
        }
    // Prefer controlled transparent artwork for brands whose adaptive launcher icons can be
    // rendered by Android with an unwanted white plate around them.
    if (bundledIcon != null) {
        Image(
            painter = painterResource(bundledIcon),
            contentDescription = "${profile.name} icon",
            modifier = Modifier.size(size),
        )
        return
    }
    val installedIcon =
        remember(profile.id) {
            profile.androidPackages.firstNotNullOfOrNull { packageName ->
                runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
            }
        }
    if (installedIcon != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = "${profile.name} icon"
                    setImageDrawable(installedIcon)
                }
            },
            update = { it.setImageDrawable(installedIcon) },
            modifier = Modifier.size(size),
        )
        return
    }
    val background =
        when (profile.id) {
            "whatsapp" -> Color(0xFF128C4A)
            "facebook" -> Color(0xFF1877F2)
            "x" -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFFF2F2F2) else Color.Black
            "tiktok" -> Color(0xFF010101)
            else -> MaterialTheme.colorScheme.primary
        }
    val foreground = if (background.luminance() > 0.7f) Color.Black else Color.White
    Surface(shape = CircleShape, color = background, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            when (profile.id) {
                "whatsapp" ->
                    Icon(
                        Icons.Rounded.Call,
                        contentDescription = "WhatsApp icon",
                        tint = foreground,
                        modifier = Modifier.size(size * .5f),
                    )
                else ->
                    Text(
                        text = profile.monogram,
                        color = foreground,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
            }
        }
    }
}
