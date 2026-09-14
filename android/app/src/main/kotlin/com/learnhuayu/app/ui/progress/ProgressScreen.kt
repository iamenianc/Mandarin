package com.learnhuayu.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.app.R
import com.learnhuayu.core.ui.PinyinText

/**
 * Progress (FR-34, WF-5). Audio-first: the learner reads the practice history and recurring
 * themes, then taps one action to hear the spoken summary. Pinyin is shown with tone numbers
 * only (ADR 0011, ADR 0012).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgressScreen(
    onBack: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.progress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.progress_loading),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            !uiState.hasHistory -> EmptyState(modifier = Modifier.padding(innerPadding))

            else -> HistoryContent(
                uiState = uiState,
                onPlaySummary = viewModel::onPlaySummary,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.progress_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.progress_empty_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.qol_progress_empty_action),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryContent(
    uiState: ProgressUiState,
    onPlaySummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.progress_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        uiState.message?.let { message ->
            item(key = "message") {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item(key = "summary") {
            SummaryCard(uiState = uiState, onPlaySummary = onPlaySummary)
        }
        item(key = "feedback-themes") {
            ThemeSection(
                title = stringResource(R.string.progress_feedback_themes_title),
                themes = uiState.feedbackThemes,
            )
        }
        item(key = "debrief-themes") {
            ThemeSection(
                title = stringResource(R.string.progress_debrief_themes_title),
                themes = uiState.debriefThemes,
            )
        }
        item(key = "most-practised") {
            MostPractisedSection(items = uiState.mostPractised)
        }
    }
}

@Composable
private fun SummaryCard(uiState: ProgressUiState, onPlaySummary: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.progress_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            uiState.summaryText?.let { summary ->
                Text(text = summary, style = MaterialTheme.typography.bodyLarge)
            }
            if (uiState.summaryText == null && uiState.message == null) {
                Text(
                    text = stringResource(R.string.qol_progress_no_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onPlaySummary,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canPlaySummary,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.playing) R.string.progress_playing_summary else R.string.progress_play_summary,
                    ),
                )
            }
            if (!uiState.canPlaySummary && !uiState.playing) {
                Text(
                    text = stringResource(R.string.qol_progress_no_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.progress_focus_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (uiState.focusAreas.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_focus_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.focusAreas.forEach { area ->
                    Text(text = area, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(title: String, themes: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (themes.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_no_themes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            themes.forEach { theme ->
                Text(text = theme, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun MostPractisedSection(items: List<PractisedItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.progress_most_practised_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.qol_progress_most_practised_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        items.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PinyinText(pinyin = item.pinyin, style = MaterialTheme.typography.titleMedium)
                    if (item.meaning.isNotBlank()) {
                        Text(
                            text = item.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.progress_times_practised, item.timesPracticed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
