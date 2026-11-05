package dev.brandonvargas.conceptgames.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.ui.components.StarRow
import dev.brandonvargas.conceptgames.ui.components.meta
import dev.brandonvargas.conceptgames.ui.games.GameScreen
import dev.brandonvargas.conceptgames.ui.navigation.PlayRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(route: PlayRoute, onExit: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PlayViewModel(container.packs, route) }
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val playing = phase is PlayPhase.Playing

    BackHandler(enabled = playing) { confirmExit = true }

    val title = when (val current = phase) {
        is PlayPhase.Playing -> {
            val name = stringResource(current.round.gameId.meta().name)
            if (viewModel.isMixed) "$name · ${current.index + 1}/${viewModel.playlist.size}" else name
        }
        is PlayPhase.Finished -> stringResource(R.string.results_title)
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { if (playing) confirmExit = true else onExit() }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = phase,
                contentKey = { current ->
                    when (current) {
                        is PlayPhase.Playing -> "play_${current.roundNumber}"
                        else -> current::class.simpleName
                    }
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "play_phase",
            ) { current ->
                when (current) {
                    PlayPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    PlayPhase.Missing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.hub_missing)) }
                    is PlayPhase.Playing -> GameScreen(current.round, viewModel::finish)
                    is PlayPhase.BetweenGames -> BetweenGames(current, onNext = viewModel::next)
                    is PlayPhase.Finished -> Results(
                        results = current.results,
                        pack = viewModel.pack,
                        onPlayAgain = viewModel::playAgain,
                        onDone = onExit,
                    )
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.play_quit_title)) },
            text = { Text(stringResource(R.string.play_quit_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExit()
                }) { Text(stringResource(R.string.play_quit)) }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.play_keep_playing)) } },
        )
    }
}

@Composable
private fun BetweenGames(phase: PlayPhase.BetweenGames, onNext: () -> Unit) {
    CenteredColumn {
        StarRow(phase.last.stars, size = 48.dp)
        Text(
            stringResource(R.string.results_score, phase.last.correct, phase.last.total),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.play_next_up), style = MaterialTheme.typography.labelLarge)
        val meta = phase.next.meta()
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(meta.icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(meta.name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(meta.description), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("next_game")) {
            Text(stringResource(R.string.play_next))
        }
    }
}

@Composable
private fun Results(results: List<GameResult>, pack: StudyPack?, onPlayAgain: () -> Unit, onDone: () -> Unit) {
    val correct = results.sumOf { it.correct }
    val total = results.sumOf { it.total }
    val overall = GameResult(results.first().gameId, correct, total)
    val missed = results.flatMap { it.missedConceptIds }.distinct().mapNotNull { pack?.concept(it) }
    CenteredColumn {
        StarRow(overall.stars, size = 56.dp)
        Text(
            stringResource(
                when (overall.stars) {
                    3 -> R.string.results_perfect
                    2 -> R.string.results_great
                    1 -> R.string.results_good
                    else -> R.string.results_keep_going
                },
            ),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.results_score, correct, total), style = MaterialTheme.typography.titleLarge)
        if (results.size > 1) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { result ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(result.gameId.meta().icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(result.gameId.meta().name), modifier = Modifier.weight(1f))
                            Text("${result.correct}/${result.total}", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            StarRow(result.stars)
                        }
                    }
                }
            }
        }
        if (missed.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.results_review), style = MaterialTheme.typography.titleMedium)
                    missed.forEachIndexed { index, concept ->
                        if (index > 0) HorizontalDivider()
                        Text(concept.term, style = MaterialTheme.typography.titleSmall)
                        Text(concept.definition, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("play_again")) {
            Text(stringResource(R.string.results_play_again))
        }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("results_done")) {
            Text(stringResource(R.string.results_back))
        }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}
