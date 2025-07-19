package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider
import br.com.rodorush.chartpatterntracker.util.loadImageUrlFromStorage
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockPatternListProvider
import br.com.rodorush.chartpatterntracker.viewmodel.ScreeningViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectChartPatternScreen(
    viewModel: ScreeningViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val patternProvider = LocalPatternProvider.current
    var patterns by remember { mutableStateOf<List<PatternItem>>(emptyList()) }
    val selectedPatterns by viewModel.selectedPatterns.collectAsState()

    LaunchedEffect(Unit) {
        patternProvider.fetchPatterns { loadedPatterns ->
            patterns = loadedPatterns
            Log.d("SelectChartPatternScreen", "Padrões carregados: ${loadedPatterns.map { it.id to it.getLocalized("name") }}")
        }
    }

    val checkStates = remember(patterns, selectedPatterns) {
        mutableStateListOf<Boolean>().apply {
            addAll(patterns.map { pattern ->
                selectedPatterns.any { it.id == pattern.id }
            })
        }
    }
    var searchText by remember { mutableStateOf("") }
    val filteredIndices = remember(searchText, patterns) {
        patterns.mapIndexedNotNull { index, pattern ->
            if (pattern.getLocalized("name").contains(searchText, ignoreCase = true)) index else null
        }
    }
    var allChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Ação do perfil */ }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.profile)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.search_patterns)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val selected = patterns.filterIndexed { index, _ -> checkStates[index] }
                    Button(onClick = {
                        viewModel.updateSelectedPatterns(selected)
                        Log.d("SelectChartPatternScreen", "Botão Home clicado, padrões selecionados: ${selected.map { it.id to it.getLocalized("name") }}")
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.home))
                    }
                    Button(
                        onClick = {
                        viewModel.updateSelectedPatterns(selected)
                        Log.d("SelectChartPatternScreen", "Botão Avançar clicado, padrões selecionados: ${selected.map { it.id to it.getLocalized("name") }}")
                        onNextClick()
                        },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.next))
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.next)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_the_chart_pattern),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredIndices) { index ->
                    val pattern = patterns[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkStates[index],
                            onCheckedChange = { isChecked ->
                                checkStates[index] = isChecked
                                Log.d("SelectChartPatternScreen", "Checkbox alterado: ${pattern.id} - ${pattern.getLocalized("name")} = $isChecked")
                            }
                        )
                        Text(
                            text = pattern.getLocalized("name"),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        var imageUrl by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(pattern.id) {
                            imageUrl = loadImageUrlFromStorage(pattern.id)
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(id = R.drawable.castical_64px),
                            contentDescription = stringResource(R.string.pattern_description),
                            modifier = Modifier
                                .width(40.dp)
                                .clickable {
                                    onNavigateToDetails(pattern.id)
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.step_1_of_3))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = allChecked,
                    onCheckedChange = { toggled ->
                        allChecked = toggled
                        for (i in checkStates.indices) {
                            checkStates[i] = toggled
                        }
                        Log.d("SelectChartPatternScreen", "Switch allChecked alterado: $toggled")
                    }
                )
                Text(
                    stringResource(
                        R.string.all_none
                    ) + " - " +
                        stringResource(R.string.total_count, patterns.size)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectChartPatternPreview() {
    ChartPatternTrackerTheme {
        CompositionLocalProvider(LocalPatternProvider provides MockPatternListProvider()) {
            SelectChartPatternScreen()
        }
    }
}