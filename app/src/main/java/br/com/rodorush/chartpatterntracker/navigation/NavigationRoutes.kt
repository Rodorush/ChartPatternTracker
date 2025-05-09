package br.com.rodorush.chartpatterntracker.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object ChartPatternDetail : Screen("chart_pattern_detail") {
        fun createRoute(patternId: String) = "chart_pattern_detail/$patternId"
    }
    object SelectChartPattern : Screen("select_chart_pattern")
    object SelectAssets : Screen("select_assets")
    object SelectTimeframes : Screen("select_timeframes")
    object ScreeningResults : Screen("screening_results")
    object ChartDetail : Screen("chart_detail") {
        fun createRoute(ticker: String, timeframe: String) = "chart_detail/$ticker/$timeframe"
    }
    object Settings : Screen("settings") // Nova rota para Settings
}