package dev.paperlib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeasurementTest {
    @Test
    fun `A4 copy paper weighs about five grams`() {
        val sheet = PaperSheet.standard()
        // 210 x 297 mm = 0.06237 m², at 80 g/m² that is 4.9896 g.
        assertEquals(0.06237, sheet.areaSquareMetres, absoluteTolerance = 1e-5)
        assertEquals(4.9896, sheet.mass.value, absoluteTolerance = 1e-3)
    }

    @Test
    fun `A series sizes keep the sqrt two ratio`() {
        listOf(PaperSize.A0, PaperSize.A1, PaperSize.A2, PaperSize.A3, PaperSize.A4, PaperSize.A5)
            .forEach { size ->
                assertEquals(kotlin.math.sqrt(2.0), size.aspectRatio, absoluteTolerance = 1e-3)
            }
    }

    @Test
    fun `letter and legal are US sizes`() {
        assertEquals(215.9, PaperSize.LETTER.portraitWidth.value, absoluteTolerance = 1e-9)
        assertEquals(355.6, PaperSize.LEGAL.portraitHeight.value, absoluteTolerance = 1e-9)
        assertEquals(1.294, PaperSize.LETTER.areaSquareMetres, absoluteTolerance = 1e-3)
    }

    @Test
    fun `landscape swaps the trim without changing area`() {
        val portrait = PaperSheet(PaperSize.A4, PaperStock.COPY_80)
        val landscape = portrait.copy(landscape = true)
        assertEquals(297.0, landscape.width.value, absoluteTolerance = 1e-9)
        assertEquals(210.0, landscape.height.value, absoluteTolerance = 1e-9)
        assertEquals(portrait.areaSquareMetres, landscape.areaSquareMetres, absoluteTolerance = 1e-9)
        assertEquals(portrait.mass.value, landscape.mass.value, absoluteTolerance = 1e-9)
    }

    @Test
    fun `caliper comes from basis weight over density`() {
        // 80 g/m² at 0.80 g/cm³: 100 cm³ per m², so 0.1 mm.
        assertEquals(100.0, PaperStock.COPY_80.thickness.value, absoluteTolerance = 1e-6)
        // 250 g/m² at 0.85 g/cm³ ≈ 0.294 mm, typical folding box board.
        assertEquals(294.1, PaperStock.BOARD_250.thickness.value, absoluteTolerance = 0.2)
    }

    @Test
    fun `a ream of A4 80gsm is about two and a half kilos`() {
        val ream = PaperReam.of(PaperSheet.standard())
        assertEquals(500, ream.sheetCount)
        assertEquals(2494.8, ream.totalMass.value, absoluteTolerance = 0.5)
        assertEquals(50.0, ream.stackHeight.value, absoluteTolerance = 0.01)
        assertEquals(31.185, ream.totalArea, absoluteTolerance = 0.001)
    }

    @Test
    fun `weight targeted reams round down to whole sheets`() {
        val sheet = PaperSheet.standard()
        val ream = PaperReam.forKilograms(sheet, 3.0)
        assertEquals(601, ream.sheetCount)
        assertTrue(ream.totalMass.toKilograms() <= 3.0)
        assertTrue(ream.totalMass.toKilograms() > 2.9)
    }

    @Test
    fun `nearest basis weight picks the closest stock`() {
        assertEquals(PaperStock.BOARD_250, PaperStock.nearestBasisWeight(256.0))
        assertEquals(PaperStock.NEWSPRINT_45, PaperStock.nearestBasisWeight(48.0))
        assertEquals(PaperStock.COPY_80, PaperStock.nearestBasisWeight(80.0))
    }

    @Test
    fun `size lookup is case insensitive`() {
        assertEquals(PaperSize.A3, PaperSize.fromNameOrNull("a3"))
        assertEquals(PaperSize.LETTER, PaperSize.fromNameOrNull("Letter"))
        assertEquals(null, PaperSize.fromNameOrNull("B5"))
    }
}