package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import br.com.rodorush.chartpatterntracker.ui.screen.ChartPatternDetailScreen
import br.com.rodorush.chartpatterntracker.ui.screen.MainScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SelectAssetsScreen
import br.com.rodorush.chartpatterntracker.ui.screen.SelectChartPatternScreen
import br.com.rodorush.chartpatterntracker.util.LocalAssetsProvider
import br.com.rodorush.chartpatterntracker.util.provider.BrapiAssetsProvider

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
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
                onLogout = onLogout
            )
        }

        composable(Screen.SelectChartPattern.route) {
            SelectChartPatternScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNextClick = {
                    navController.navigate(Screen.SelectAssets.route)
                },
                onNavigateToDetails = { patternId ->
                    navController.navigate(Screen.ChartPatternDetail.createRoute(patternId))
                }
            )
        }

        composable(Screen.SelectAssets.route) {
            CompositionLocalProvider(LocalAssetsProvider provides BrapiAssetsProvider()) {
                SelectAssetsScreen(
                    onNavigateBack = {
                        navController.popBackStack() // Volta para SelectChartPattern
                    },
                    onNextClick = {
                        // Aqui você pode adicionar a navegação para a próxima tela (Passo 3) quando estiver pronta
                    }
                )
            }
        }

        composable(
            route = "chart_pattern_detail/{patternId}",
            arguments = listOf(navArgument("patternId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patternId = backStackEntry.arguments?.getString("patternId") ?: ""

            ChartPatternDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                patternId = patternId
            )
        }
    }
}