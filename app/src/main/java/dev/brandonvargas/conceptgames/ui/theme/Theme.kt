package dev.brandonvargas.conceptgames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import dev.brandonvargas.conceptgames.engine.games.GameId

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFFBF7FF),
    onBackground = Color(0xFF1B1A23),
    surface = Color(0xFFFBF7FF),
    onSurface = Color(0xFF1B1A23),
    surfaceVariant = Color(0xFFE7E5F4),
    onSurfaceVariant = Color(0xFF474556),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FD),
    surfaceContainer = Color(0xFFF0ECF9),
    surfaceContainerHigh = Color(0xFFEAE6F4),
    surfaceContainerHighest = Color(0xFFE4E0EE),
    outline = Color(0xFF79768A),
    outlineVariant = Color(0xFFCAC6DA),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF13111C),
    onBackground = Color(0xFFE6E1F0),
    surface = Color(0xFF13111C),
    onSurface = Color(0xFFE6E1F0),
    surfaceVariant = Color(0xFF464455),
    onSurfaceVariant = Color(0xFFC9C5D9),
    surfaceContainerLowest = Color(0xFF0E0C16),
    surfaceContainerLow = Color(0xFF1B1925),
    surfaceContainer = Color(0xFF1F1D29),
    surfaceContainerHigh = Color(0xFF2A2834),
    surfaceContainerHighest = Color(0xFF35333F),
    outline = Color(0xFF938FA3),
    outlineVariant = Color(0xFF464455),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

/** Colors with a meaning in games: right/wrong feedback and one accent per minigame. */
@Immutable
data class GameColors(
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val onWrongContainer: Color,
    val accents: List<Pair<Color, Color>>,
) {
    /** (container, content) accent for a game tile. */
    fun accent(id: GameId): Pair<Color, Color> = accents[id.ordinal % accents.size]
}

private val LightGameColors = GameColors(
    correct = Color(0xFF16A34A),
    correctContainer = Color(0xFFDCFCE7),
    onCorrectContainer = Color(0xFF052E16),
    wrong = Color(0xFFDC2626),
    wrongContainer = Color(0xFFFEE2E2),
    onWrongContainer = Color(0xFF450A0A),
    accents = listOf(
        Color(0xFFE0E7FF) to Color(0xFF3730A3),
        Color(0xFFCCFBF1) to Color(0xFF115E59),
        Color(0xFFFEF3C7) to Color(0xFF92400E),
        Color(0xFFFFE4E6) to Color(0xFF9F1239),
        Color(0xFFE0F2FE) to Color(0xFF075985),
        Color(0xFFF3E8FF) to Color(0xFF6B21A8),
        Color(0xFFECFCCB) to Color(0xFF3F6212),
    ),
)

private val DarkGameColors = GameColors(
    correct = Color(0xFF4ADE80),
    correctContainer = Color(0xFF14532D),
    onCorrectContainer = Color(0xFFDCFCE7),
    wrong = Color(0xFFF87171),
    wrongContainer = Color(0xFF7F1D1D),
    onWrongContainer = Color(0xFFFEE2E2),
    accents = listOf(
        Color(0xFF312E81) to Color(0xFFE0E7FF),
        Color(0xFF134E4A) to Color(0xFFCCFBF1),
        Color(0xFF78350F) to Color(0xFFFEF3C7),
        Color(0xFF881337) to Color(0xFFFFE4E6),
        Color(0xFF0C4A6E) to Color(0xFFE0F2FE),
        Color(0xFF581C87) to Color(0xFFF3E8FF),
        Color(0xFF365314) to Color(0xFFECFCCB),
    ),
)

val LocalGameColors = staticCompositionLocalOf { LightGameColors }

private val BaseTypography = Typography()
private val AppTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun ConceptGamesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGameColors provides if (darkTheme) DarkGameColors else LightGameColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

object GameTheme {
    val colors: GameColors
        @Composable get() = LocalGameColors.current
}
