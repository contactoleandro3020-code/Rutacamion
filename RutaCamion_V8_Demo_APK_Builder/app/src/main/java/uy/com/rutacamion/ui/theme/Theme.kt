package uy.com.rutacamion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RutaColors = lightColorScheme(
    primary = Color(0xFF0B63CE),
    onPrimary = Color.White,
    secondary = Color(0xFF083B7A),
    background = Color(0xFFF4F7FB),
    surface = Color.White,
    error = Color(0xFFD32F2F)
)

@Composable
fun RutaCamionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RutaColors, content = content)
}
