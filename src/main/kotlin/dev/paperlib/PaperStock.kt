package dev.paperlib

/**
 * Surface finish, which drives both bulk and how the sheet shades when drawn.
 */
enum class PaperFinish(val label: String) {
    SMOOTH("smooth"),
    LAID("laid"),
    LINEN("linen"),
    NEWSPRINT("newsprint"),
    RECYCLED("recycled"),
    CARDBOARD("cardboard");

    /** How much the drawn sheet is broken up by texture. */
    val textureStrength: Int
        get() = when (this) {
            SMOOTH -> 0
            LAID, LINEN -> 2
            NEWSPRINT, RECYCLED -> 1
            CARDBOARD -> 1
        }
}

/**
 * A paper stock: basis weight, material density, optical brightness and finish.
 *
 * Densities are typical mid-range values for uncoated and coated printing
 * papers; they are what convert basis weight into a caliper.
 */
data class PaperStock(
    val name: String,
    val basisWeight: GramsPerSquareMeter,
    val density: GramsPerCubicCentimetre,
    val brightness: Int,
    val finish: PaperFinish,
    val whitePoint: Int = DEFAULT_WHITE_POINT,
) {
    init {
        require(brightness in 0..100) { "ISO brightness must be 0..100, was $brightness" }
        require(whitePoint in 0..100) { "white point must be 0..100, was $whitePoint" }
    }

    /**
     * Caliper: basis weight divided by density, in micrometres.
     *
     * 1 m² of 80 g/m² paper at 0.80 g/cm³ occupies 100 cm³, which is 0.1 mm thick.
     */
    val thickness: Micrometres
        get() = Micrometres(basisWeight.value / density.value)

    /** Roughness proxy: 0 is glassy, 1 is fully matte. */
    val matteFactor: Double
        get() = when (finish) {
            PaperFinish.SMOOTH -> 0.05
            PaperFinish.LAID -> 0.55
            PaperFinish.LINEN -> 0.5
            PaperFinish.NEWSPRINT -> 0.7
            PaperFinish.RECYCLED -> 0.75
            PaperFinish.CARDBOARD -> 0.6
        }

    companion object {
        const val DEFAULT_WHITE_POINT = 96

        fun of(
            name: String,
            basisWeight: Double,
            density: Double,
            brightness: Int = 84,
            finish: PaperFinish = PaperFinish.SMOOTH,
        ): PaperStock = PaperStock(
            name = name,
            basisWeight = GramsPerSquareMeter(basisWeight),
            density = GramsPerCubicCentimetre(density),
            brightness = brightness,
            finish = finish,
        )

        val COPY_80 = of("Copy 80", 80.0, 0.80, 84, PaperFinish.SMOOTH)
        val PREMIUM_120 = of("Premium 120", 120.0, 0.78, 92, PaperFinish.SMOOTH)
        val OFFSET_90 = of("Offset 90", 90.0, 0.82, 80, PaperFinish.LINEN)
        val NEWSPRINT_45 = of("Newsprint 45", 45.0, 0.65, 58, PaperFinish.NEWSPRINT)
        val RECYCLED_100 = of("Recycled 100", 100.0, 0.72, 72, PaperFinish.RECYCLED)
        val BOARD_250 = of("Board 250", 250.0, 0.85, 74, PaperFinish.CARDBOARD)

        /** Nearest stock to a basis weight, for CLI convenience. */
        fun nearestBasisWeight(gsm: Double): PaperStock =
            listOf(COPY_80, PREMIUM_120, OFFSET_90, NEWSPRINT_45, RECYCLED_100, BOARD_250)
                .minBy { kotlin.math.abs(it.basisWeight.value - gsm) }
    }
}