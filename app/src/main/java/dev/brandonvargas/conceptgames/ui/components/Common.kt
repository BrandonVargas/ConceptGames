package dev.brandonvargas.conceptgames.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameId

/** Display info for each minigame. */
data class GameMeta(@StringRes val name: Int, @StringRes val description: Int, val icon: ImageVector)

fun GameId.meta(): GameMeta = when (this) {
    GameId.MultipleChoice -> GameMeta(R.string.game_multiple_choice, R.string.game_multiple_choice_desc, Icons.Rounded.Quiz)
    GameId.RopeMatch -> GameMeta(R.string.game_rope_match, R.string.game_rope_match_desc, Icons.AutoMirrored.Rounded.CompareArrows)
    GameId.WordScramble -> GameMeta(R.string.game_word_scramble, R.string.game_word_scramble_desc, Icons.Rounded.Abc)
    GameId.Crossword -> GameMeta(R.string.game_crossword, R.string.game_crossword_desc, Icons.Rounded.Grid4x4)
    GameId.Flashcards -> GameMeta(R.string.game_flashcards, R.string.game_flashcards_desc, Icons.Rounded.FlipToBack)
    GameId.TrueFalse -> GameMeta(R.string.game_true_false, R.string.game_true_false_desc, Icons.Rounded.SwipeRight)
    GameId.FillBlank -> GameMeta(R.string.game_fill_blank, R.string.game_fill_blank_desc, Icons.Rounded.ShortText)
    GameId.Timeline -> GameMeta(R.string.game_timeline, R.string.game_timeline_desc, Icons.AutoMirrored.Rounded.Sort)
    GameId.WordSearch -> GameMeta(R.string.game_word_search, R.string.game_word_search_desc, Icons.Rounded.GridOn)
    GameId.Hangman -> GameMeta(R.string.game_hangman, R.string.game_hangman_desc, Icons.Rounded.Air)
    GameId.MemoryPairs -> GameMeta(R.string.game_memory, R.string.game_memory_desc, Icons.Rounded.Style)
    GameId.Categorize -> GameMeta(R.string.game_categorize, R.string.game_categorize_desc, Icons.Rounded.Category)
    GameId.OddOneOut -> GameMeta(R.string.game_odd_one_out, R.string.game_odd_one_out_desc, Icons.Rounded.SearchOff)
}

/** Opens a link in a Custom Tab, falling back to any browser. */
fun Context.openUrl(url: String) {
    val uri = url.toUri()
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return { url -> context.openUrl(url) }
}

@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 16.dp, color: Color = MaterialTheme.colorScheme.tertiary) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(3) { index ->
            Icon(
                if (index < stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
fun LanguageBadge(code: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            code.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun Difficulty.label(): String = stringResource(
    when (this) {
        Difficulty.Easy -> R.string.difficulty_easy
        Difficulty.Medium -> R.string.difficulty_medium
        Difficulty.Hard -> R.string.difficulty_hard
    },
)
