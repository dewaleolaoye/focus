package com.usefocus.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.usefocus.app.BlockerApplication

@Composable
fun WebsiteIcon(domain: String, size: Dp = 48.dp) {
    val application = LocalContext.current.applicationContext as BlockerApplication
    val bitmap by produceState<android.graphics.Bitmap?>(null, domain) {
        value = runCatching { application.websiteIcons.load(domain) }.getOrNull()
    }
    val shape = RoundedCornerShape(size * .24f)
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "$domain website icon",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier.size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface),
        )
    } else {
        Box(
            modifier =
                Modifier.size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Language, contentDescription = "$domain website icon")
        }
    }
}
