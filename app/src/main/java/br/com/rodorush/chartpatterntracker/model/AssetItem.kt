package br.com.rodorush.chartpatterntracker.model

private val WHITESPACE = "\\s+".toRegex()

data class AssetItem(
    val ticker: String,
    val shortName: String = "",
    val longName: String = "",
    // Mantido por compatibilidade e como fallback quando só há 'name' no /quote/list
    val name: String = "",
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val logo: String = ""
) {
    /**
     * Nome mostrado abaixo do ticker na seleção de ativos e nos resultados da varredura: o
     * primeiro preenchido entre [shortName], [name] e [longName], com os espaços normalizados.
     *
     * Um valor igual ao ticker não conta, porque repetiria a linha de cima. A Brapi devolve o
     * ticker no lugar do nome em vários casos: no `shortName` da cotação individual e no `name`
     * da maioria dos BDRs no `/quote/list`. Vazio quando nenhum dos três serve.
     *
     * Calculado na construção, uma vez por instância: a busca da seleção o lê em todos os ativos
     * da lista a cada tecla. Fica fora do `equals` e do `copy`, que o recalcula.
     */
    val displayName: String = sequenceOf(shortName, name, longName)
        .map { it.trim().replace(WHITESPACE, " ") }
        .firstOrNull { it.isNotEmpty() && !it.equals(ticker.trim(), ignoreCase = true) }
        .orEmpty()
}
