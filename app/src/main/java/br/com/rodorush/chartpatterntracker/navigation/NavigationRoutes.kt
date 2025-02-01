package br.com.rodorush.chartpatterntracker.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object SelectChartPattern : Screen("select_chart_pattern")
}