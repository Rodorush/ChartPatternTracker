package br.com.rodorush.chartpatterntracker.model.pattern

object PatternDetectorRegistry {
    private val detectors = mutableMapOf<String, PatternDetector>()

    init {
        register("1", AbandonedBabyBearishDetector())
        register("9", BearishEngulfingDetector())
        register("10", EveningDojiStarDetector())
        register("11", EveningStarDetector())
        register("24", ThreeOutsideDownDetector())
        register("43", BullishDojiStarDetector())
        register("46", BullishHammerDetector())
        register("48", HaramiBullishDetector())
        register("58", ThreeOutsideUpDetector())
    }

    fun register(id: String, detector: PatternDetector) {
        detectors[id] = detector
    }

    fun get(id: String): PatternDetector? = detectors[id]

    fun registeredIds(): List<String> = detectors.keys.toList()
}
