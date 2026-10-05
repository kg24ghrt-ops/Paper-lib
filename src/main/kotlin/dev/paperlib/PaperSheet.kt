package dev.paperlib

/**
 * One trimmed sheet: a size, an orientation and a stock.
 */
data class PaperSheet(
    val size: PaperSize,
    val stock: PaperStock,
    val landscape: Boolean = false,
) {
    val width: Millimetres
        get() = if (landscape) size.portraitHeight else size.portraitWidth

    val height: Millimetres
        get() = if (landscape) size.portraitWidth else size.portraitHeight

    val areaSquareMetres: Double
        get() = width.value * height.value / 1_000_000.0

    /**
     * Mass of a single sheet.
     *
     * A sheet covers `areaSquareMetres` of the m² the basis weight is defined
     * per, so the mass scales by exactly that fraction.
     */
    val mass: Grams
        get() = Grams(areaSquareMetres * stock.basisWeight.value)

    val thickness: Micrometres
        get() = stock.thickness

    fun describe(): String = buildString {
        append(size.name)
        if (landscape) append(" landscape")
        append(" · ")
        append(stock.name)
        append(" · ")
        append(width)
        append(" × ")
        append(height)
    }

    companion object {
        /** Sheets in an ISO 500 ream. */
        const val SHEETS_PER_REAM = 500

        /** Sheets in the short reams used for heavier stocks. */
        const val SHEETS_PER_SHORT_REAM = 250

        fun of(size: PaperSize, stock: PaperStock, landscape: Boolean = false): PaperSheet =
            PaperSheet(size, stock, landscape)

        fun standard(size: PaperSize = PaperSize.A4, stock: PaperStock = PaperStock.COPY_80): PaperSheet =
            PaperSheet(size, stock)
    }
}

/**
 * A ream of a given sheet: a count and the stack it forms.
 */
data class PaperReam(
    val sheet: PaperSheet,
    val sheetCount: Int,
) {
    init {
        require(sheetCount > 0) { "a ream needs at least one sheet, was $sheetCount" }
    }

    val totalMass: Grams
        get() = Grams(sheet.mass.value * sheetCount)

    /** Stack height of the whole ream. */
    val stackHeight: Micrometres
        get() = Micrometres(sheet.thickness.value * sheetCount)

    val totalArea: Double
        get() = sheet.areaSquareMetres * sheetCount

    companion object {
        fun of(sheet: PaperSheet, sheetCount: Int = PaperSheet.SHEETS_PER_REAM): PaperReam =
            PaperReam(sheet, sheetCount)

        /**
         * How many sheets of [stock] make a ream of [targetKilograms],
         * so a packer can hit a shipping weight instead of a fixed count.
         */
        fun forKilograms(sheet: PaperSheet, targetKilograms: Double): PaperReam {
            require(targetKilograms > 0.0) { "target weight must be positive" }
            val perSheetKg = sheet.mass.toKilograms()
            val count = (targetKilograms / perSheetKg).toInt().coerceAtLeast(1)
            return PaperReam(sheet, count)
        }
    }
}