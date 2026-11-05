package dev.brandonvargas.conceptgames.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.data.wikipedia.WikiArticle
import dev.brandonvargas.conceptgames.ui.components.toErrorInfo
import dev.brandonvargas.conceptgames.ui.navigation.PickerRoute
import dev.brandonvargas.conceptgames.ui.navigation.SelectedArticle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(route: PickerRoute, onBack: () -> Unit, onGenerate: (List<SelectedArticle>) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { PickerViewModel(container.wikipedia, container.settings, route.topic, route.language) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.picker_title))
                        Text(route.topic, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            val canGenerate = state.selected.isNotEmpty() || (state.webSearch && !state.loading)
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onGenerate(state.selectedArticles().map { SelectedArticle(it.pageId, it.title, it.url) }) },
                    enabled = canGenerate,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_games"),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(pluralStringResource(R.plurals.picker_create, state.selected.size, state.selected.size))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(48.dp))
                state.error != null -> Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val info = state.error!!.toErrorInfo()
                    Text(stringResource(info.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(info.message), textAlign = TextAlign.Center)
                    OutlinedButton(onClick = viewModel::search) { Text(stringResource(R.string.action_retry)) }
                }
                else -> LazyColumn(
                    modifier = Modifier.widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.picker_help, PickerViewModel.MAX_SELECTED),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.webSearch) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.picker_web_on), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (state.articles.isEmpty()) {
                        item { Text(stringResource(R.string.picker_empty), style = MaterialTheme.typography.bodyLarge) }
                    }
                    items(state.articles, key = { it.pageId }) { article ->
                        ArticleCard(
                            article = article,
                            selected = article.pageId in state.selected,
                            enabled = article.pageId in state.selected || state.selected.size < PickerViewModel.MAX_SELECTED,
                            onToggle = { viewModel.toggle(article.pageId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleCard(article: WikiArticle, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    OutlinedCard(onClick = onToggle, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (article.thumbnailUrl != null) {
                AsyncImage(
                    model = article.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Surface(Modifier.size(56.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) { Text(article.title.take(1), style = MaterialTheme.typography.titleLarge) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium)
                article.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
        }
    }
}
