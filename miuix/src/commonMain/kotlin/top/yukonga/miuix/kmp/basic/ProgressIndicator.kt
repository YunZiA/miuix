import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.More

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color,
    rotation: Float = 0f
) {
    Crossfade(
        targetState = Unit,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = MiuixIcons.More,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        }
    }
}