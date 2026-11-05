package dev.brandonvargas.conceptgames.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.BuildConfig
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.ui.components.label
import dev.brandonvargas.conceptgames.ui.components.rememberUrlOpener
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

private const val REPO_URL = "https://github.com/BrandonVargas/ConceptGames"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenKeyGuide: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { SettingsViewModel(container.settings, container.gemini) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberUrlOpener()
    val settings = state.settings ?: return

    LaunchedEffect(settings.hasApiKey) { if (settings.hasApiKey) viewModel.loadModels() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Section(stringResource(R.string.settings_ai))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (settings.hasApiKey) Icons.Rounded.CheckCircle else Icons.Rounded.Key,
                        contentDescription = null,
                        tint = if (settings.hasApiKey) GameTheme.colors.correct else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(if (settings.hasApiKey) R.string.settings_key_saved else R.string.settings_key_missing),
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = onOpenKeyGuide) { Text(stringResource(R.string.settings_manage_key)) }
                }
                Text(
                    stringResource(R.string.settings_key_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ModelPicker(selected = settings.model, models = state.models, onSelect = viewModel::setModel)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_web_search), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.settings_web_search_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.webSearch, onCheckedChange = viewModel::setWebSearch)
                }

                HorizontalDivider()
                Section(stringResource(R.string.settings_default_difficulty))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == settings.difficulty,
                            onClick = { viewModel.setDifficulty(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                        ) { Text(option.label()) }
                    }
                }

                HorizontalDivider()
                Section(stringResource(R.string.settings_about))
                Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyMedium)
                Link(Icons.Rounded.Code, stringResource(R.string.settings_source_code)) { openUrl(REPO_URL) }
                Link(Icons.Rounded.Policy, stringResource(R.string.settings_privacy)) { openUrl("$REPO_URL/blob/main/PRIVACY.md") }
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Link(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(selected: String, models: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_model)) },
            supportingText = { Text(stringResource(R.string.settings_model_help)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(text = { Text(model) }, onClick = {
                    onSelect(model)
                    expanded = false
                })
            }
        }
    }
}
