package dev.brandonvargas.conceptgames.ui.generate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.data.gemini.GeminiException
import dev.brandonvargas.conceptgames.domain.GenerationStep
import dev.brandonvargas.conceptgames.engine.validate.PackProgress
import dev.brandonvargas.conceptgames.ui.components.toErrorInfo
import dev.brandonvargas.conceptgames.ui.navigation.GenerateRoute
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay
import java.io.IOException
import java.text.NumberFormat

@Composable
fun GenerateScreen(route: GenerateRoute, onBack: () -> Unit, onDone: (Long) -> Unit, onFixKey: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { GenerateViewModel(container.generatePack, container.languages, route) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    // Ticks once a second while working, driving the elapsed timers and the retry countdown.
    val now by produceState(System.currentTimeMillis(), state.running) {
        while (state.running) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }

    LaunchedEffect(state.packId) { state.packId?.let(onDone) }
    LaunchedEffect(state.cancelled) { if (state.cancelled) onBack() }
    BackHandler(enabled = state.running) { confirmCancel = true }

    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Sparkle(animate = state.running)
            Text(stringResource(R.string.generate_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(route.topic, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().animateContentSize(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepRow(
                        label = stringResource(R.string.generate_step_wikipedia),
                        state = stateOf(GenerationStep.ReadingWikipedia, state),
                        detail = state.articles?.let { count ->
                            pluralStringResource(
                                R.plurals.generate_read_articles,
                                count,
                                count,
                                NumberFormat.getIntegerInstance().format(state.characters),
                            )
                        },
                    )
                    StepRow(
                        label = stringResource(R.string.generate_step_web),
                        state = stateOf(GenerationStep.SearchingWeb, state),
                        detail = when {
                            state.webSkipped -> webSkipMessage(state.webSkipReason)
                            state.webSources != null -> pluralStringResource(R.plurals.generate_web_pages, state.webSources!!, state.webSources!!)
                            else -> null
                        },
                        elapsed = elapsedIf(GenerationStep.SearchingWeb, state, now),
                    )
                    StepRow(
                        label = stringResource(R.string.generate_step_games),
                        state = stateOf(GenerationStep.WritingGames, state),
                        elapsed = elapsedIf(GenerationStep.WritingGames, state, now),
                    ) {
                        if (state.current == GenerationStep.WritingGames && state.running) {
                            WritingDetails(state, now)
                        }
                    }
                    StepRow(stringResource(R.string.generate_step_saving), stateOf(GenerationStep.Saving, state))
                }
            }

            WaitingBanner(state.waitingUntil, now)

            val error = state.error
            if (error == null) {
                Text(
                    stringResource(R.string.generate_wait_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (state.running) {
                    OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.testTag("cancel_generation")) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            } else {
                val info = error.toErrorInfo()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(info.title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(info.message), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (info.fixKey) {
                    Button(onClick = onFixKey, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.error_fix_key)) }
                }
                OutlinedButton(onClick = viewModel::start, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_retry))
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.generate_cancel_title)) },
            text = { Text(stringResource(R.string.generate_cancel_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    viewModel.cancel()
                }) { Text(stringResource(R.string.generate_cancel_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.generate_keep_waiting)) }
            },
        )
    }
}

/** What Gemini is doing right now: its latest thought, and what it has written so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WritingDetails(state: GenerateState, now: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val progress = state.progress
        if (progress == null || progress.items == 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    state.thought?.let { stringResource(R.string.generate_thinking, it) } ?: stringResource(R.string.generate_planning),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (progress != null && progress.items > 0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProgressChip(R.plurals.progress_concepts, progress.concepts)
                ProgressChip(R.plurals.progress_facts, progress.facts)
                ProgressChip(R.plurals.progress_questions, progress.questions)
                ProgressChip(R.plurals.progress_sentences, progress.clozes)
                ProgressChip(R.plurals.progress_sequences, progress.timelines)
                ProgressChip(R.plurals.progress_groups, progress.categories)
                ProgressChip(R.plurals.progress_odd_sets, progress.oddSets)
            }
        }
        if (now - state.stepStartedAt > SLOW_AFTER_MS) {
            Text(
                stringResource(R.string.generate_slow_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressChip(label: Int, count: Int) {
    if (count == 0) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            pluralStringResource(label, count, count),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun WaitingBanner(waitingUntil: Long?, now: Long) {
    val seconds = waitingUntil?.let { ((it - now) / 1000).coerceAtLeast(0) }
    AnimatedVisibility(seconds != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HourglassTop, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.generate_waiting, (seconds ?: 0).toInt()), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun webSkipMessage(reason: Throwable?): String = stringResource(
    when (reason) {
        null -> R.string.generate_web_off
        is GeminiException.NotOnFreeTier -> R.string.generate_web_not_free
        is GeminiException.Quota -> R.string.generate_web_quota
        is GeminiException.ModelNotFound -> R.string.generate_web_model
        is GeminiException.Network, is IOException -> R.string.generate_web_offline
        else -> R.string.generate_web_failed
    },
)

private enum class StepState { Pending, Running, Done, Skipped, Failed }

private fun stateOf(step: GenerationStep, state: GenerateState): StepState {
    if (step == GenerationStep.SearchingWeb && state.webSkipped) return StepState.Skipped
    val current = state.current ?: return StepState.Pending
    return when {
        step < current -> StepState.Done
        step == current && state.error != null -> StepState.Failed
        step == current -> if (state.packId != null) StepState.Done else StepState.Running
        else -> StepState.Pending
    }
}

private fun elapsedIf(step: GenerationStep, state: GenerateState, now: Long): String? {
    if (state.current != step || !state.running) return null
    val seconds = ((now - state.stepStartedAt) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun StepRow(
    label: String,
    state: StepState,
    detail: String? = null,
    elapsed: String? = null,
    content: @Composable () -> Unit = {},
) {
    val colors = GameTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StepState.Running -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                StepState.Done -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.correct)
                StepState.Skipped -> Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                StepState.Failed -> Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = colors.wrong)
                StepState.Pending -> Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state == StepState.Pending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (elapsed != null) {
                    Text(elapsed, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun Sparkle(animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "sparkle")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "rotation")
    val scale by transition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .rotate(if (animate) rotation else 0f)
                    .scale(if (animate) scale else 1f),
            )
        }
    }
}

private const val SLOW_AFTER_MS = 45_000L
