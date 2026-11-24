package br.com.rodorush.chartpatterntracker.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
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
import br.com.rodorush.customlogs.Log
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

private const val TAG = "SelectAssetsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAssetsScreen(
    viewModel: ScreeningViewModel,
    onNavigateBack: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onChartClick: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val assetsProvider = LocalAssetsProvider.current
    val context = LocalContext.current
    var assets by remember { mutableStateOf<List<AssetItem>>(emptyList()) }
    val selectedAssets by viewModel.selectedAssets.collectAsState()

    LaunchedEffect(Unit) {
        assetsProvider.fetchAssets(useLastLoad = true) { loadedAssets ->
            val filtered = loadedAssets.filterNot { it.ticker.endsWith("F", ignoreCase = true) }
            assets = filtered.sortedBy { it.ticker }
            Log.d(
                TAG,
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

    // Ao abrir o gráfico ou o Perfil, a tela sai da composição e perde os checkStates; o
    // ViewModel os guarda. Só quando a seleção mudou: atualizar cancela a varredura e a refaz.
    // Com a lista ainda vazia, ou recém-chegada e sem recomposição, os checkStates não a cobrem:
    // não há o que guardar, e a seleção vazia apagaria a salva.
    fun saveSelectionIfChanged() {
        if (assets.isEmpty() || checkStates.size != assets.size) return
        val selected = assets.filterIndexed { i, _ -> checkStates[i] }
        val saved = viewModel.selectedAssets.value
        if (selected.map { it.ticker }.toSet() != saved.map { it.ticker }.toSet()) {
            viewModel.updateSelectedAssets(selected)
        }
    }
    var searchText by remember { mutableStateOf("") }
    val filteredIndices = remember(searchText, assets) {
        assets.mapIndexedNotNull { index, asset ->
            if (asset.ticker.contains(searchText, ignoreCase = true) ||
                asset.displayName.contains(searchText, ignoreCase = true)) index else null
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
                        IconButton(onClick = {
                            saveSelectionIfChanged()
                            onNavigateToProfile()
                        }) {
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
                    IconButton(onClick = {
                        viewModel.updateSelectedAssets(selected)
                        viewModel.setAllAssetsChecked(allChecked)
                        Log.d(
                            TAG,
                            "Botão Voltar clicado, ativos selecionados: ${selected.map { it.ticker }}"
                        )
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.updateSelectedAssets(selected)
                        Log.d(
                            TAG,
                            "Botão Home clicado, ativos selecionados: ${selected.map { it.ticker }}"
                        )
                        onNavigateHome()
                    }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = stringResource(R.string.home),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (selected.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.select_at_least_one_asset),
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.d(TAG, "Avançar sem seleção")
                                return@IconButton
                            }
                            viewModel.updateSelectedAssets(selected)
                            viewModel.setAllAssetsChecked(allChecked)
                            Log.d(
                                TAG,
                                "Botão Avançar clicado, ativos selecionados: ${selected.map { it.ticker }}"
                            )
                            onNextClick()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.size(32.dp)
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
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredIndices) { index ->
                    val asset = assets[index]
                    val rowDisplayName = asset.displayName
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
                                    TAG,
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
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(text = asset.ticker)
                            if (rowDisplayName.isNotEmpty()) {
                                Text(
                                    text = rowDisplayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        IconButton(onClick = {
                            saveSelectionIfChanged()
                            onChartClick(asset.ticker)
                        }) {
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
                            TAG,
                            "Switch allChecked alterado: $toggled"
                        )
                    }
                )
                val selectedCount = checkStates.count { it }
                Text(
                    stringResource(R.string.total_count, selectedCount, assets.size)
                )
            }
        }
    }
}
