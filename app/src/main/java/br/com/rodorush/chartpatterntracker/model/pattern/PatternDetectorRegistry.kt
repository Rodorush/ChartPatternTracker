package br.com.rodorush.chartpatterntracker.model.pattern

object PatternDetectorRegistry {
    private val detectors = mutableMapOf<String, PatternDetector>()

    init {
        // Bebê Abandonado de Baixa - document ID 1
        register("1", BebeAbandonadoBaixaDetector())
        // Estrela Doji da Noite - document ID 10
        register("10", EstrelaDojiDaNoiteDetector())
        // Estrela da Noite - document ID 11
        register("11", EstrelaDaNoiteDetector())
        // Três Fora de Baixa - document ID 24
        register("24", TresForaDeBaixaDetector())
        // Estrela Doji de Alta - document ID 43
        register("43", EstrelaDojiDeAltaDetector())
        // Martelo de Alta - document ID 46
        register("46", MarteloDeAltaDetector())
        // Três Fora de Alta - document ID 58
        register("58", TresForaDeAltaDetector())
        // Harami de Alta - document ID 48
        register("48", HaramiAltaDetector())
        // Bearish Engulfing pattern - document ID 9
        register("9", BearishEngulfingDetector())
    }

    fun register(id: String, detector: PatternDetector) {
        detectors[id] = detector
    }

    fun get(id: String): PatternDetector? = detectors[id]

    fun registeredIds(): List<String> = detectors.keys.toList()
}
