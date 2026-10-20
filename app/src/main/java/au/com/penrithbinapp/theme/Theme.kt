package au.com.penrithbinapp.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF003911),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFD6E8FF),
    background = Color(0xFF101518),
    surface = Color(0xFF182026),
    surfaceVariant = Color(0xFF232F38)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6E2D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F5DC),
    onPrimaryContainer = Color(0xFF082910),
    secondary = Color(0xFF2E5B78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9EAF7),
    onSecondaryContainer = Color(0xFF0E2636),
    background = Color(0xFFF6F8F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EFE9)
)

@Composable
fun PenrithBinAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
