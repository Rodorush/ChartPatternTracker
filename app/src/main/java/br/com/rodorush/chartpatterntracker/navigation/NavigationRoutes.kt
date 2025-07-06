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
        fun createRoute(
            ticker: String,
            timeframe: String,
            patternId: String? = null,
            detectPatterns: Boolean = true
        ): String {
            val idPart = patternId ?: ""
            return "chart_detail/$ticker/$timeframe?patternId=$idPart&detectPatterns=$detectPatterns"
        }
    }
    object RealTimeQuotes : Screen("real_time_quotes")
    object Settings : Screen("settings") // Nova rota para Settings
}