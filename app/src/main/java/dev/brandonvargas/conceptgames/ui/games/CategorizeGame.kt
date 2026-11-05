package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.CategorizeRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Sort a deck of items into buckets: drag the top card onto a bucket, or tap a bucket. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorizeGame(round: CategorizeRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    val scope = rememberCoroutineScope()
    val colors = GameTheme.colors
    var index by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var placed by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var lastWrong by remember { mutableStateOf<Int?>(null) }
    var lastRight by remember { mutableStateOf<Int?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    val bucketBounds = remember { mutableStateMapOf<Int, Rect>() }
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val finished = index >= round.items.size

    fun place(bucket: Int) {
        if (finished) return
        val item = round.items[index]
        if (item.categoryIndex == bucket) {
            if (lastWrong == null) correct++
            lastRight = bucket
            lastWrong = null
            placed = placed + index
            index++
            feedback.right()
        } else {
            // A miss costs this item's point; the player still has to find the right bucket.
            lastWrong = bucket
            shake++
            feedback.wrong()
        }
        scope.launch { drag.animateTo(Offset.Zero, spring()) }
    }

    LaunchedEffect(lastRight, placed.size) {
        if (lastRight != null) {
            delay(400)
            lastRight = null
        }
    }
    LaunchedEffect(finished) {
        if (finished) {
            delay(500)
            onFinish(GameResult(GameId.Categorize, correct, round.items.size, durationMs = elapsedSince(start)))
        }
    }

    GameColumn {
        GameProgress(index, round.items.size)
        Text(stringResource(R.string.categorize_instructions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
            modifier = Modifier.fillMaxWidth(),
        ) {
            round.categories.forEachIndexed { bucket, name ->
                val (container, content) = GameTheme.colors.accent(GameId.entries[(bucket * 3 + 1) % GameId.entries.size])
                val members = placed.map { round.items[it] }.filter { it.categoryIndex == bucket }
                Surface(
                    onClick = { place(bucket) },
                    shape = RoundedCornerShape(18.dp),
                    color = when (bucket) {
                        lastRight -> colors.correctContainer
                        lastWrong -> colors.wrongContainer
                        else -> container
                    },
                    contentColor = content,
                    border = BorderStroke(2.dp, if (bucket == lastWrong) colors.wrong else content.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 110.dp)
                        .onGloballyPositioned { bucketBounds[bucket] = it.boundsInRoot() }
                        .testTag("bucket_$bucket"),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        members.forEach { Text("• ${it.text}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
            if (!finished) {
                val item = round.items[index]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .zIndex(1f)
                        .shakeOn(shake)
                        .graphicsLayer {
                            translationX = drag.value.x
                            translationY = drag.value.y
                        }
                        .onGloballyPositioned { cardBounds = it.boundsInRoot() }
                        .pointerInput(index) {
                            detectDragGestures(
                                onDragEnd = {
                                    val center = cardBounds.center
                                    val target = bucketBounds.entries.firstOrNull { it.value.contains(center) }?.key
                                    if (target != null) place(target) else scope.launch { drag.animateTo(Offset.Zero, spring()) }
                                },
                            ) { change, amount ->
                                change.consume()
                                scope.launch { drag.snapTo(drag.value + amount) }
                            }
                        }
                        .testTag("categorize_card"),
                ) {
                    Text(
                        item.text,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                    )
                }
            }
        }
        if (lastWrong != null && !finished) {
            Row { Text(stringResource(R.string.categorize_try_again), color = colors.wrong, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
