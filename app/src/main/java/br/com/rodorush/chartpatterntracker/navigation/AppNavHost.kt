package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import br.com.rodorush.chartpatterntracker.model.ChartInterval
import br.com.rodorush.chartpatterntracker.ui.screen.*
import br.com.rodorush.chartpatterntracker.util.LocalAssetsProvider
import br.com.rodorush.chartpatterntracker.util.provider.BrapiAssetsProvider
import br.com.rodorush.chartpatterntracker.viewmodel.ScreeningViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val screeningViewModel: ScreeningViewModel = viewModel()

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
                    navController.navigate(Screen.Settings.route) // Navegação para Settings
                },
                onLogout = onLogout
            )
        }

        composable(Screen.SelectChartPattern.route) {
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
            CompositionLocalProvider(LocalAssetsProvider provides BrapiAssetsProvider()) {
                SelectAssetsScreen(
                    viewModel = screeningViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNextClick = { navController.navigate(Screen.SelectTimeframes.route) }
                )
            }
        }

        composable(Screen.SelectTimeframes.route) {
            SelectTimeframesScreen(
                viewModel = screeningViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNextClick = { navController.navigate(Screen.ScreeningResults.route) }
            )
        }

        composable(Screen.ScreeningResults.route) {
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
            val timeframeStr = backStackEntry.arguments?.getString("timeframe") ?: "1d"
            val timeframe = ChartInterval.fromString(timeframeStr)
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

        composable(Screen.Settings.route) { // Nova rota para Settings
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}