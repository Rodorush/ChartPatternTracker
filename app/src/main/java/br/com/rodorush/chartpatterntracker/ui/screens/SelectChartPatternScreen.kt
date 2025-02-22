package br.com.rodorush.chartpatterntracker.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.navigation.Screen
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.utils.LocalPatternProvider
import br.com.rodorush.chartpatterntracker.utils.providers.MockPatternProvider
import com.google.firebase.firestore.DocumentId
import java.util.Locale

data class PatternItem(
    @DocumentId
    val id: String = "",
    val name: Map<String, String> = emptyMap(),
    val isChecked: Boolean = false
)

fun getLocalizedName(nameMap: Map<String, String>): String {
    val locale = Locale.getDefault().language // "pt", "en", etc.
    return nameMap[locale] ?: nameMap["pt"] ?: "Padrão desconhecido"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectChartPatternScreen(
    onNavigateBack: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {}
) {
    val patternProvider = LocalPatternProvider.current
    var patterns by remember { mutableStateOf<List<PatternItem>>(emptyList()) }
    val checkStates = remember { mutableStateListOf<Boolean>() }
    var searchText by remember { mutableStateOf("") }
    var allChecked by remember { mutableStateOf(false) }

    // Carregar dados do Firestore
    LaunchedEffect(Unit) {
        patternProvider.fetchPatterns { loadedPatterns ->
            patterns = loadedPatterns
            checkStates.clear()
            checkStates.addAll(List(loadedPatterns.size) { false })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Row com ícone de perfil e campo de busca
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Ação do perfil, se houver */ }) {
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
            // Barra inferior com botões Home e Avançar
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onNavigateBack) {
                        Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.home))
                    }
                    Button(onClick = onNextClick) {
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
        // Conteúdo principal
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título principal
            Text(
                text = stringResource(R.string.select_the_chart_pattern),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Lista de checkboxes
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f) // ocupa espaço disponível
            ) {
                itemsIndexed(patterns) { index, pattern ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp), // Adiciona um pequeno padding lateral
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox e nome do padrão
                        Checkbox(
                            checked = checkStates[index],
                            onCheckedChange = { isChecked ->
                                checkStates[index] = isChecked
                            }
                        )
                        Text(
                            text = getLocalizedName(pattern.name),
                            modifier = Modifier.weight(1f) // Garante que o texto ocupa o espaço necessário
                        )

                        // Espaço flexível para empurrar o ícone para a direita
                        Spacer(modifier = Modifier.width(16.dp))

                        // Ícone alinhado à direita
                        Image(
                            painter = painterResource(id = R.drawable.castical_64px),
                            contentDescription = stringResource(R.string.pattern_description),
                            modifier = Modifier
                                .width(40.dp) // Define um tamanho fixo para a coluna do ícone
                                .clickable {
                                    onNavigateToDetails(pattern.id)
                                }
                        )
                    }
                }
            }

            // Área inferior com passo e switch
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.step_1_de_3))

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
                    }
                )
                Text(stringResource(R.string.all_none))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectChartPatternPreview() {
    ChartPatternTrackerTheme {
        CompositionLocalProvider(LocalPatternProvider provides MockPatternProvider()) {
            SelectChartPatternScreen()
        }
    }
}