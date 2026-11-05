package dev.brandonvargas.conceptgames.ui.onboarding

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.ui.components.rememberUrlOpener
import dev.brandonvargas.conceptgames.ui.components.toErrorInfo
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

const val AI_STUDIO_KEY_URL = "https://aistudio.google.com/apikey"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyGuideScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ApiKeyGuideViewModel(container.settings, container.gemini) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberUrlOpener()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.key_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        // imePadding shrinks the scroll area above the keyboard so the key field and buttons stay reachable.
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IntroCard()

                Step(1, stringResource(R.string.key_step_open_title), stringResource(R.string.key_step_open_body)) {
                    FilledTonalButton(onClick = { openUrl(AI_STUDIO_KEY_URL) }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.key_open_ai_studio))
                    }
                }
                Step(2, stringResource(R.string.key_step_create_title), stringResource(R.string.key_step_create_body))
                Step(3, stringResource(R.string.key_step_copy_title), stringResource(R.string.key_step_copy_body))
                Step(4, stringResource(R.string.key_step_paste_title), stringResource(R.string.key_step_paste_body)) {
                    KeyEntry(
                        state = state,
                        onKeyChange = viewModel::onKeyChange,
                        onPaste = { context.clipboardText()?.let(viewModel::onKeyChange) },
                        onTest = viewModel::testAndSave,
                    )
                }

                if (state.status is KeyStatus.Valid || state.hasSavedKey) {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("key_done")) {
                        Text(stringResource(R.string.key_done))
                    }
                }

                Text(
                    stringResource(R.string.key_tips_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Tip(Icons.Rounded.Lock, R.string.key_tip_private_title, R.string.key_tip_private_body)
                Tip(Icons.Rounded.Savings, R.string.key_tip_free_title, R.string.key_tip_free_body)
                Tip(Icons.Rounded.Policy, R.string.key_tip_privacy_title, R.string.key_tip_privacy_body)
                Tip(Icons.Rounded.Sync, R.string.key_tip_revoke_title, R.string.key_tip_revoke_body)
                Tip(Icons.Rounded.HourglassBottom, R.string.key_tip_quota_title, R.string.key_tip_quota_body)

                val guideUrl = stringResource(R.string.key_guide_url)
                TextButton(onClick = { openUrl(guideUrl) }) {
                    Text(stringResource(R.string.key_full_guide))
                }
                if (state.hasSavedKey) {
                    OutlinedButton(onClick = viewModel::removeKey, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.key_remove))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.key_intro_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.key_intro_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String, content: (@Composable () -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number.toString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content?.invoke()
            }
        }
    }
}

@Composable
private fun KeyEntry(
    state: KeyGuideState,
    onKeyChange: (String) -> Unit,
    onPaste: () -> Unit,
    onTest: () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = state.key,
        onValueChange = onKeyChange,
        label = { Text(stringResource(R.string.key_field_label)) },
        placeholder = { Text("AIza…") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (state.key.isNotBlank() && state.status != KeyStatus.Testing) onTest() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (visible) R.string.key_hide else R.string.key_show),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("key_field"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPaste) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.key_paste))
        }
        Button(onClick = onTest, enabled = state.key.isNotBlank() && state.status != KeyStatus.Testing) {
            Text(stringResource(R.string.key_test))
        }
    }
    KeyStatusView(state.status)
}

@Composable
private fun KeyStatusView(status: KeyStatus) {
    val colors = GameTheme.colors
    // The verdict appears below the fold on small phones, so scroll it into view.
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(status) {
        if (status is KeyStatus.Valid || status is KeyStatus.Invalid) requester.bringIntoView()
    }
    val modifier = Modifier.bringIntoViewRequester(requester)
    when (status) {
        KeyStatus.Idle -> Unit
        KeyStatus.Testing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.key_testing))
        }
        is KeyStatus.Valid -> StatusCard(
            icon = Icons.Rounded.CheckCircle,
            title = stringResource(R.string.key_valid_title),
            body = pluralStringResource(R.plurals.key_valid_body, status.modelCount, status.modelCount),
            container = colors.correctContainer,
            content = colors.onCorrectContainer,
            modifier = modifier,
        )
        is KeyStatus.Invalid -> {
            val info = status.error.toErrorInfo()
            StatusCard(
                icon = Icons.Rounded.ErrorOutline,
                title = stringResource(info.title),
                body = stringResource(info.message),
                container = colors.wrongContainer,
                content = colors.onWrongContainer,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = container, contentColor = content, modifier = modifier.testTag("key_status")) {
        Row(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Tip(icon: ImageVector, title: Int, body: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Context.clipboardText(): String? {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
}
