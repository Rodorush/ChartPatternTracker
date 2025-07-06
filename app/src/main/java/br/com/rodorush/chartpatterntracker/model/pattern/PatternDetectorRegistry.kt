package br.com.rodorush.chartpatterntracker.model.pattern

object PatternDetectorRegistry {
    private val detectors = mutableMapOf<String, PatternDetector>()

    init {
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
