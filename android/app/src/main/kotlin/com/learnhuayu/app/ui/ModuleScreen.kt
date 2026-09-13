package com.learnhuayu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.learnhuayu.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(
    moduleId: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val module = viewModel.module(moduleId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module?.title ?: stringResource(R.string.module_not_found)) },
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
            if (module == null) {
                Text(text = stringResource(R.string.module_not_found))
            } else {
                Section(
                    title = stringResource(R.string.lessons_title),
                    emptyText = stringResource(R.string.empty_lessons),
                    items = module.lessons().map { it.title },
                )
                Section(
                    title = stringResource(R.string.practice_title),
                    emptyText = stringResource(R.string.empty_practice),
                    items = module.practices().map { it.title },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Section(
    title: String,
    emptyText: String,
    items: List<String>,
) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    if (items.isEmpty()) {
        Text(text = emptyText, style = MaterialTheme.typography.bodyMedium)
    } else {
        items.forEach { item ->
            Text(text = item, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
