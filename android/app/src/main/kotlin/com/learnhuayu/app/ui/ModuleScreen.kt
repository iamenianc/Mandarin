package com.learnhuayu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnhuayu.app.R
import com.learnhuayu.app.ui.session.SessionKind
import com.learnhuayu.core.ui.LargeTapTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(
    moduleId: String,
    onSpecClick: (SessionKind, String) -> Unit,
    onBack: () -> Unit,
    viewModel: ModuleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(moduleId) {
        viewModel.load(moduleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title ?: stringResource(R.string.module_not_found)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                uiState.loading -> Text(
                    text = stringResource(R.string.module_loading),
                    style = MaterialTheme.typography.bodyLarge,
                )

                uiState.notFound -> Text(
                    text = stringResource(R.string.module_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )

                else -> {
                    Section(
                        title = stringResource(R.string.lessons_title),
                        emptyText = stringResource(R.string.empty_lessons),
                        items = uiState.lessons.map { it.id to it.title },
                        onItemClick = { specId -> onSpecClick(SessionKind.LESSON, specId) },
                    )
                    Section(
                        title = stringResource(R.string.practice_title),
                        emptyText = stringResource(R.string.empty_practice),
                        items = uiState.practices.map { it.id to it.title },
                        onItemClick = { specId -> onSpecClick(SessionKind.PRACTICE, specId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Section(
    title: String,
    emptyText: String,
    items: List<Pair<String, String>>,
    onItemClick: (String) -> Unit,
) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    if (items.isEmpty()) {
        Text(text = emptyText, style = MaterialTheme.typography.bodyMedium)
    } else {
        items.forEach { (id, itemTitle) ->
            LargeTapTarget(
                label = itemTitle,
                onClick = { onItemClick(id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
