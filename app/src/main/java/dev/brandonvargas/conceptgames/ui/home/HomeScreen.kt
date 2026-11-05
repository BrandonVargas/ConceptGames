package dev.brandonvargas.conceptgames.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.data.db.PackSummary
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.ui.components.LanguageBadge
import dev.brandonvargas.conceptgames.ui.components.label
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import java.text.DateFormat
import java.util.Date
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFindSources: (topic: String, language: String, difficulty: Difficulty) -> Unit,
    onOpenPack: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onSetUpKey: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { HomeViewModel(container.settings, container.packs, container.languages) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var topic by rememberSaveable { mutableStateOf("") }
    var showLanguages by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val settings = state.settings ?: return
    val hasKey = settings.hasApiKey
    val submit = {
        if (!hasKey) onSetUpKey() else if (topic.isNotBlank()) onFindSources(topic.trim(), settings.contentLanguage, settings.difficulty)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TopicCard(
                        topic = topic,
                        onTopicChange = { topic = it },
                        languageName = state.language?.nativeName ?: settings.contentLanguage,
                        difficulty = settings.difficulty,
                        hasKey = hasKey,
                        onLanguageClick = {
                            showLanguages = true
                            viewModel.loadAllLanguages()
                        },
                        onDifficulty = viewModel::setDifficulty,
                        onSubmit = submit,
                        onSetUpKey = onSetUpKey,
                    )
                }
                if (topic.isBlank()) {
                    item { Suggestions(onPick = { topic = it }) }
                }
                item {
                    Text(
                        stringResource(R.string.home_library),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.library.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.home_library_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.library, key = { it.id }) { pack ->
                    PackCard(pack, onClick = { onOpenPack(pack.id) }, onDelete = { pendingDelete = pack.id })
                }
            }
        }
    }

    if (showLanguages) {
        LanguageSheet(
            languages = state.languages,
            selected = settings.contentLanguage,
            onSelect = {
                viewModel.setLanguage(it)
                showLanguages = false
            },
            onDismiss = { showLanguages = false },
        )
    }

    pendingDelete?.let { id ->
        val pack = state.library.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.home_delete_title)) },
            text = { Text(stringResource(R.string.home_delete_body, pack?.topic.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TopicCard(
    topic: String,
    onTopicChange: (String) -> Unit,
    languageName: String,
    difficulty: Difficulty,
    hasKey: Boolean,
    onLanguageClick: () -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onSubmit: () -> Unit,
    onSetUpKey: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_prompt), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                placeholder = { Text(stringResource(R.string.home_topic_placeholder)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth().testTag("topic_field"),
            )
            AssistChip(
                onClick = onLanguageClick,
                label = { Text(stringResource(R.string.home_language, languageName)) },
                leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == difficulty,
                        onClick = { onDifficulty(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                    ) { Text(option.label()) }
                }
            }
            if (hasKey) {
                Button(
                    onClick = onSubmit,
                    enabled = topic.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("find_sources"),
                ) { Text(stringResource(R.string.home_find_sources)) }
            } else {
                Text(stringResource(R.string.home_no_key), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onSetUpKey, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_set_up_key))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.home_suggestions), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.topic_suggestions).forEach { suggestion ->
                SuggestionChip(onClick = { onPick(suggestion) }, label = { Text(suggestion) })
            }
        }
    }
}

@Composable
private fun PackCard(pack: PackSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    val (container, content) = GameTheme.colors.accent(GameId.entries[pack.topic.hashCode().absoluteValue % GameId.entries.size])
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("pack_${pack.topic}")) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(container, CircleShape), contentAlignment = Alignment.Center) {
                Text(pack.topic.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = content)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pack.topic, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LanguageBadge(pack.language)
                    val detail = if (pack.isSample) {
                        stringResource(R.string.home_sample)
                    } else {
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(pack.createdAt))
                    }
                    Text(
                        "$detail · ${pluralStringResource(R.plurals.home_plays, pack.plays, pack.plays)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}
