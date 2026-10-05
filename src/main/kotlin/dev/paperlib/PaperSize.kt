package dev.paperlib

/**
 * Trimmed sheet sizes in millimetres. Width and height are always the portrait
 * trim; use [PaperSheet.width] and [PaperSheet.height] for oriented dimensions.
 */
enum class PaperSize(val portraitWidth: Millimetres, val portraitHeight: Millimetres) {
    A0(Millimetres(841.0), Millimetres(1189.0)),
    A1(Millimetres(594.0), Millimetres(841.0)),
    A2(Millimetres(420.0), Millimetres(594.0)),
    A3(Millimetres(297.0), Millimetres(420.0)),
    A4(Millimetres(210.0), Millimetres(297.0)),
    A5(Millimetres(148.0), Millimetres(210.0)),
    LETTER(Millimetres(215.9), Millimetres(279.4)),
    LEGAL(Millimetres(215.9), Millimetres(355.6));

    /** Trim area in square metres, the basis for basis-weight maths. */
    val areaSquareMetres: Double
        get() = portraitWidth.value * portraitHeight.value / 1_000_000.0

    /** Long edge over short edge; the A-series ratio is sqrt(2). */
    val aspectRatio: Double
        get() = portraitHeight.value / portraitWidth.value

    companion object {
        fun fromNameOrNull(name: String): PaperSize? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}