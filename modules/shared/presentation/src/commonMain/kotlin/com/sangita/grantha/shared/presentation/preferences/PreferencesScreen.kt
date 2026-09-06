package com.sangita.grantha.shared.presentation.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sangita.grantha.shared.domain.model.ScriptCodeDto
import com.sangita.grantha.shared.mobile.storage.AppearancePreference
import com.sangita.grantha.shared.mobile.storage.TextSizePreference
import com.sangita.grantha.shared.presentation.RasikaCopy
import com.sangita.grantha.shared.presentation.components.RasikaChip
import com.sangita.grantha.shared.presentation.components.RasikaScreenHeader
import com.sangita.grantha.shared.presentation.theme.RasikaTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    presenter: PreferencesPresenter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        RasikaScreenHeader(
            title = RasikaCopy.PREFERENCES,
            onBack = onBack,
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RasikaTokens.screen, vertical = RasikaTokens.md),
        ) {
            PreferenceGroup(RasikaCopy.APPEARANCE) {
                AppearancePreference.entries.forEach { value ->
                    RasikaChip(
                        selected = state.appearance == value,
                        onClick = { presenter.setAppearance(value) },
                        label = value.displayName(),
                    )
                }
            }
            PreferenceGroup(RasikaCopy.TEXT_SIZE) {
                TextSizePreference.entries.forEach { value ->
                    RasikaChip(
                        selected = state.textSize == value,
                        onClick = { presenter.setTextSize(value) },
                        label = value.displayName(),
                    )
                }
            }
            PreferenceGroup(RasikaCopy.SCRIPT) {
                RasikaChip(
                    selected = state.preferredScript == null,
                    onClick = { presenter.setScript(null) },
                    label = RasikaCopy.SERVER_DEFAULT,
                )
                ScriptCodeDto.entries.forEach { script ->
                    RasikaChip(
                        selected = state.preferredScript == script,
                        onClick = { presenter.setScript(script) },
                        label = script.name.lowercase().replaceFirstChar { it.titlecase() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = RasikaTokens.md, bottom = RasikaTokens.xs),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
        verticalArrangement = Arrangement.spacedBy(RasikaTokens.xs),
    ) {
        content()
    }
}

private fun AppearancePreference.displayName(): String = when (this) {
    AppearancePreference.SYSTEM -> "System"
    AppearancePreference.LIGHT -> "Light"
    AppearancePreference.DARK -> "Dark"
}

private fun TextSizePreference.displayName(): String = when (this) {
    TextSizePreference.SMALL -> "Small"
    TextSizePreference.MEDIUM -> "Medium"
    TextSizePreference.LARGE -> "Large"
    TextSizePreference.EXTRA_LARGE -> "Extra large"
}
