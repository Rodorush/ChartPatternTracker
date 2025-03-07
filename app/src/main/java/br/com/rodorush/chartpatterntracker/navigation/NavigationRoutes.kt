package br.com.rodorush.chartpatterntracker.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object SelectChartPattern : Screen("select_chart_pattern")
    object SelectAssets : Screen("select_assets")
    object SelectTimeframes : Screen("select_timeframes")
    object ChartPatternDetail : Screen("chart_pattern_detail") {
        fun createRoute(patternId: String) = "chart_pattern_detail/$patternId"
    }
}