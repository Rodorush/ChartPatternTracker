package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.LocalAssetsProvider
import br.com.rodorush.chartpatterntracker.viewmodel.ScreeningViewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAssetsScreen(
    viewModel: ScreeningViewModel,
    onNavigateBack: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onChartClick: (String) -> Unit = {}
) {
    val assetsProvider = LocalAssetsProvider.current
    var assets by remember { mutableStateOf<List<AssetItem>>(emptyList()) }
    val selectedAssets by viewModel.selectedAssets.collectAsState()

    LaunchedEffect(Unit) {
        assetsProvider.fetchAssets { loadedAssets ->
            val filtered = loadedAssets.filterNot { it.ticker.endsWith("F", ignoreCase = true) }
            assets = filtered.sortedBy { it.ticker }
            Log.d(
                "SelectAssetsScreen",
                "Ativos carregados: ${assets.map { it.ticker }}"
            )
        }
    }

    val checkStates = remember(assets, selectedAssets) {
        mutableStateListOf<Boolean>().apply {
            addAll(assets.map { asset ->
                selectedAssets.any { it.ticker == asset.ticker }
            })
        }
    }
    var searchText by remember { mutableStateOf("") }
    val filteredIndices = remember(searchText, assets) {
        assets.mapIndexedNotNull { index, asset ->
            if (asset.ticker.contains(searchText, ignoreCase = true)) index else null
        }
    }
    val allChecked by viewModel.allAssetsChecked.collectAsState()

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
                            placeholder = { Text(stringResource(R.string.search_assets)) },
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
                    val selected = assets.filterIndexed { index, _ -> checkStates[index] }
                    Button(onClick = {
                        viewModel.updateSelectedAssets(selected)
                        viewModel.setAllAssetsChecked(allChecked)
                        Log.d(
                            "SelectAssetsScreen",
                            "Botão Voltar clicado, ativos selecionados: ${selected.map { it.ticker }}"
                        )
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.back))
                    }
                    Button(onClick = {
                        viewModel.updateSelectedAssets(selected)
                        Log.d(
                            "SelectAssetsScreen",
                            "Botão Home clicado, ativos selecionados: ${selected.map { it.ticker }}"
                        )
                        onNavigateHome()
                    }) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.home))
                    }
                    Button(
                        onClick = {
                            viewModel.updateSelectedAssets(selected)
                            viewModel.setAllAssetsChecked(allChecked)
                            Log.d(
                                "SelectAssetsScreen",
                                "Botão Avançar clicado, ativos selecionados: ${selected.map { it.ticker }}"
                            )
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
                text = stringResource(R.string.select_the_assets),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(40.dp))
                Spacer(modifier = Modifier.width(40.dp))
                Text(text = stringResource(R.string.ticker), modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(40.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredIndices) { index ->
                    val asset = assets[index]
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
                                if (checkStates.all { it }) {
                                    viewModel.setAllAssetsChecked(true)
                                } else if (checkStates.none { it }) {
                                    viewModel.setAllAssetsChecked(false)
                                }
                                Log.d(
                                    "SelectAssetsScreen",
                                    "Checkbox alterado: ${asset.ticker} = $isChecked"
                                )
                            }
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(asset.logo)
                                .decoderFactory(SvgDecoder.Factory())
                                .build(),
                            contentDescription = stringResource(R.string.logo),
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = asset.ticker,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onChartClick(asset.ticker) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.castical_64px),
                                contentDescription = stringResource(R.string.view_chart),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.step_2_of_3))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = allChecked,
                    onCheckedChange = { toggled ->
                        viewModel.setAllAssetsChecked(toggled)
                        for (i in checkStates.indices) {
                            checkStates[i] = toggled
                        }
                        Log.d(
                            "SelectAssetsScreen",
                            "Switch allChecked alterado: $toggled"
                        )
                    }
                )
                Text(
                    stringResource(R.string.all_none) +
                            " - " + stringResource(R.string.total_count, assets.size)
                )
            }
        }
    }
}