package dev.paperlib.cli

import dev.paperlib.PaperSize
import dev.paperlib.PaperStock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParsingTest {
    @Test
    fun `defaults to A4 copy paper`() {
        val options = assertIs<Parsed.Ok>(parseArgs(emptyArray())).options
        assertEquals(PaperSize.A4, options.size)
        assertEquals(PaperStock.COPY_80, options.stock)
        assertEquals(500, options.count)
        assertTrue(options.color)
    }

    @Test
    fun `size stock and orientation parse`() {
        val options = assertIs<Parsed.Ok>(parseArgs(arrayOf("-s", "A3", "-t", "board", "-l"))).options
        assertEquals(PaperSize.A3, options.size)
        assertEquals(PaperStock.BOARD_250, options.stock)
        assertTrue(options.landscape)
    }

    @Test
    fun `gsm overrides the stock by nearest match`() {
        val options = assertIs<Parsed.Ok>(parseArgs(arrayOf("--gsm", "120"))).options
        assertEquals(PaperStock.PREMIUM_120, options.stock)
    }

    @Test
    fun `kg overrides the sheet count`() {
        val options = assertIs<Parsed.Ok>(parseArgs(arrayOf("--kg", "2.5"))).options
        assertEquals(2.5, options.kg)
    }

    @Test
    fun `plain and width parse`() {
        val options = assertIs<Parsed.Ok>(parseArgs(arrayOf("--plain", "--width", "80"))).options
        assertEquals(false, options.color)
        assertEquals(80, options.width)
    }

    @Test
    fun `help is its own outcome`() {
        assertIs<Parsed.Help>(parseArgs(arrayOf("--help")))
    }

    @Test
    fun `bad input is rejected with a message`() {
        assertEquals("unknown size 'B5', try A4", assertIs<Parsed.Error>(parseArgs(arrayOf("-s", "B5"))).message)
        assertEquals("unknown stock 'vellum', try copy", assertIs<Parsed.Error>(parseArgs(arrayOf("-t", "vellum"))).message)
        assertEquals("count must be a positive integer, got '0'", assertIs<Parsed.Error>(parseArgs(arrayOf("-c", "0"))).message)
        assertEquals("unknown argument '--wat'", assertIs<Parsed.Error>(parseArgs(arrayOf("--wat"))).message)
        assertEquals("--gsm needs a value", assertIs<Parsed.Error>(parseArgs(arrayOf("--gsm"))).message)
    }
}