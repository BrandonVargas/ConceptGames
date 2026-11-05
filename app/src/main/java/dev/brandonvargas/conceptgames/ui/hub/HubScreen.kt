package dev.brandonvargas.conceptgames.ui.hub

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameCatalog
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.model.SourceKind
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.ui.components.LanguageBadge
import dev.brandonvargas.conceptgames.ui.components.StarRow
import dev.brandonvargas.conceptgames.ui.components.label
import dev.brandonvargas.conceptgames.ui.components.meta
import dev.brandonvargas.conceptgames.ui.components.rememberUrlOpener
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(packId: Long, onBack: () -> Unit, onPlay: (List<GameId>, Difficulty) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { HubViewModel(container.packs, container.settings, packId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pack = state.pack

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pack?.topic.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(48.dp))
                pack == null -> Text(stringResource(R.string.hub_missing), Modifier.padding(32.dp))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("hub_grid"),
                ) {
                    fullWidth { SummaryCard(pack) }
                    fullWidth {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            Difficulty.entries.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = option == state.difficulty,
                                    onClick = { viewModel.setDifficulty(option) },
                                    shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                                ) { Text(option.label()) }
                            }
                        }
                    }
                    if (state.available.size >= 2) {
                        fullWidth {
                            MixedChallengeCard(onClick = {
                                val playlist = viewModel.mixedPlaylist()
                                if (playlist.isNotEmpty()) onPlay(playlist, state.difficulty)
                            })
                        }
                    }
                    fullWidth {
                        Text(
                            pluralStringResource(R.plurals.hub_games, state.available.size, state.available.size),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(GameCatalog.builders.map { it.id }, key = { it.name }) { id ->
                        GameTile(
                            id = id,
                            enabled = id in state.available,
                            stars = state.stars[id],
                            onClick = { onPlay(listOf(id), state.difficulty) },
                        )
                    }
                    fullWidth { ConceptsCard(pack) }
                    fullWidth { SourcesCard(pack) }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SummaryCard(pack: StudyPack) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageBadge(pack.language)
                Text(
                    pluralStringResource(R.plurals.hub_concepts_count, pack.concepts.size, pack.concepts.size),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                pack.summary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MixedChallengeCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(colors.primary, colors.secondary)), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
            .testTag("mixed_challenge"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.hub_mixed), style = MaterialTheme.typography.titleLarge, color = colors.onPrimary)
                Text(stringResource(R.string.hub_mixed_desc), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun GameTile(id: GameId, enabled: Boolean, stars: Int?, onClick: () -> Unit) {
    val meta = id.meta()
    val (container, content) = GameTheme.colors.accent(id)
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.heightIn(min = 150.dp).alpha(if (enabled) 1f else 0.6f).testTag("game_${id.name}"),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(meta.icon, contentDescription = null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.weight(1f))
                if (stars != null) StarRow(stars, color = content)
            }
            Text(stringResource(meta.name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (enabled) meta.description else R.string.hub_not_enough),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConceptsCard(pack: StudyPack) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.animateContentSize().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.hub_study_concepts), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                pack.concepts.forEach { concept ->
                    HorizontalDivider()
                    Text(concept.term, style = MaterialTheme.typography.titleSmall)
                    Text(concept.definition, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SourcesCard(pack: StudyPack) {
    val openUrl = rememberUrlOpener()
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.hub_sources), style = MaterialTheme.typography.titleMedium)
        pack.sources.forEach { source ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { openUrl(source.url) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (source.kind == SourceKind.Web) Icons.Rounded.Public else Icons.Rounded.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(source.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (pack.sources.any { it.kind == SourceKind.Wikipedia }) {
            Text(
                stringResource(R.string.hub_wikipedia_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { openUrl("https://creativecommons.org/licenses/by-sa/4.0/") },
            )
        }
        Text(
            stringResource(R.string.hub_ai_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
