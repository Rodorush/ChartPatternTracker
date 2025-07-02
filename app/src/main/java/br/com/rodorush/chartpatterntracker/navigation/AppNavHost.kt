package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import br.com.rodorush.chartpatterntracker.ui.screen.ChartDetailScreen
import br.com.rodorush.chartpatterntracker.ui.screen.ChartPatternDetailScreen
import br.com.rodorush.chartpatterntracker.ui.screen.MainScreen
import br.com.rodorush.chartpatterntracker.ui.screen.ScreeningResultsScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SelectAssetsScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SelectChartPatternScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SelectTimeframesScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SettingsScreen
import br.com.rodorush.chartpatterntracker.util.LocalAssetsProvider
import br.com.rodorush.chartpatterntracker.util.provider.BrapiAssetsProvider
import br.com.rodorush.chartpatterntracker.viewmodel.ScreeningViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    // Obter o ViewModelStoreOwner da atividade
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
        ?: throw IllegalStateException("No ViewModelStoreOwner available")

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSelectChartPattern = {
                    navController.navigate(Screen.SelectChartPattern.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = onLogout
            )
        }

        composable(Screen.SelectChartPattern.route) {
            // Usar a mesma instância do ScreeningViewModel
            val screeningViewModel: ScreeningViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner
            )
            SelectChartPatternScreen(
                viewModel = screeningViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNextClick = { navController.navigate(Screen.SelectAssets.route) },
                onNavigateToDetails = { patternId ->
                    navController.navigate(Screen.ChartPatternDetail.createRoute(patternId))
                }
            )
        }

        composable(Screen.SelectAssets.route) {
            val screeningViewModel: ScreeningViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner
            )
            CompositionLocalProvider(LocalAssetsProvider provides BrapiAssetsProvider()) {
                SelectAssetsScreen(
                    viewModel = screeningViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNextClick = { navController.navigate(Screen.SelectTimeframes.route) },
                    onChartClick = { ticker -> navController.navigate(Screen.ChartDetail.createRoute(ticker, "1d")) }
                )
            }
        }

        composable(Screen.SelectTimeframes.route) {
            val screeningViewModel: ScreeningViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner
            )
            SelectTimeframesScreen(
                viewModel = screeningViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNextClick = { navController.navigate(Screen.ScreeningResults.route) }
            )
        }

        composable(Screen.ScreeningResults.route) {
            val screeningViewModel: ScreeningViewModel = koinViewModel(
                viewModelStoreOwner = viewModelStoreOwner
            )
            ScreeningResultsScreen(
                viewModel = screeningViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCardClick = { ticker, timeframe ->
                    navController.navigate("chart_detail/$ticker/$timeframe")
                }
            )
        }

        composable(
            Screen.ChartDetail.route + "/{ticker}/{timeframe}",
            arguments = listOf(
                navArgument("ticker") { type = NavType.StringType },
                navArgument("timeframe") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ticker = backStackEntry.arguments?.getString("ticker") ?: ""
            val timeframe = backStackEntry.arguments?.getString("timeframe") ?: "1d"
            ChartDetailScreen(
                ticker = ticker,
                timeframe = timeframe,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChartPatternDetail.route + "/{patternId}",
            arguments = listOf(navArgument("patternId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patternId = backStackEntry.arguments?.getString("patternId") ?: ""
            ChartPatternDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                patternId = patternId
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}