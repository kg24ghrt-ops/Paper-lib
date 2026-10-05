package dev.paperlib.render

import dev.paperlib.PaperSheet
import dev.paperlib.PaperSize
import dev.paperlib.PaperStock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetRendererTest {
    private val sheet = PaperSheet.standard()

    @Test
    fun `plain render has no escape sequences`() {
        val art = SheetRenderer.render(sheet, SheetRenderer.Options(color = false, annotate = false))
        assertTrue(!art.contains('\u001B'), "plain render must be free of ANSI codes")
        assertTrue(art.lines().size >= 6)
    }

    @Test
    fun `colour render uses truecolour backgrounds`() {
        val art = SheetRenderer.render(sheet, SheetRenderer.Options(color = true, annotate = false))
        assertTrue(art.contains("[48;2;"), "expected 24-bit background codes")
        assertTrue(art.endsWith("\u001B[0m"))
    }

    @Test
    fun `rendering is deterministic for a seed`() {
        val a = SheetRenderer.render(sheet, SheetRenderer.Options(color = true, annotate = false, seed = 7))
        val b = SheetRenderer.render(sheet, SheetRenderer.Options(color = true, annotate = false, seed = 7))
        assertEquals(a, b)
    }

    @Test
    fun `cells keep the portrait aspect ratio`() {
        val (columns, rows) = SheetRenderer.cellsFor(sheet, width = 40, maxHeight = 60)
        assertEquals(40, columns)
        // A4 is 1.414 long, and cells are twice as tall as wide: 40 * 1.414 / 2 = 28.
        assertEquals(28, rows)
    }

    @Test
    fun `tall sheet is capped by the height budget`() {
        val a0 = PaperSheet(PaperSize.A0, PaperStock.COPY_80)
        val (columns, rows) = SheetRenderer.cellsFor(a0, width = 80, maxHeight = 24)
        assertEquals(24, rows)
        assertTrue(columns < 80)
    }

    @Test
    fun `ream render reports the stack`() {
        val art = SheetRenderer.render(
            dev.paperlib.PaperReam.of(sheet, 250),
            SheetRenderer.Options(color = false),
        )
        assertTrue(art.contains("250 sheets"))
        assertTrue(art.contains("kg"))
    }
}