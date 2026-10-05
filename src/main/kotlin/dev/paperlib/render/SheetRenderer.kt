package dev.paperlib.render

import dev.paperlib.PaperFinish
import dev.paperlib.PaperReam
import dev.paperlib.PaperSheet
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Terminal renderer: draws a sheet or a ream with ANSI truecolour shading, a
 * drop shadow, curled corner and edge thickness so the shape reads as a real
 * piece of paper rather than a flat rectangle.
 */
object SheetRenderer {
    private const val ESC = "\u001B["
    private const val RESET = "${ESC}0m"

    /** Terminal cells are about twice as tall as they are wide. */
    private const val CELL_ASPECT = 2.0

    data class Options(
        val width: Int = 44,
        val color: Boolean = true,
        val annotate: Boolean = true,
        val seed: Long = 20_260_105L,
    )

    fun render(sheet: PaperSheet, options: Options = Options()): String = buildString {
        append(dropShadow(sheet, options))
        append("\n")
        append(sheetFace(sheet, options))
        if (options.annotate) {
            append("\n")
            append(caption(sheet, options))
        }
    }

    fun render(ream: PaperReam, options: Options = Options()): String = buildString {
        append(stack(ream, options))
        if (options.annotate) {
            append("\n")
            append(stackCaption(ream, options))
        }
    }

    /**
     * Width in cells that keeps the sheet's aspect ratio within [maxHeight].
     */
    fun cellsFor(sheet: PaperSheet, width: Int, maxHeight: Int): Pair<Int, Int> {
        val inner = max(width, 8)
        val rows = (inner * sheet.height.value / sheet.width.value / CELL_ASPECT).roundToInt()
        return if (rows <= maxHeight) {
            inner to max(rows, 3)
        } else {
            val scaled = (maxHeight * CELL_ASPECT * sheet.width.value / sheet.height.value).roundToInt()
            scaled to maxHeight
        }
    }

    private fun sheetFace(sheet: PaperSheet, options: Options): String {
        val optics = PaperOptics(sheet.stock)
        val (columns, rows) = cellsFor(sheet, options.width, 60)
        val grain = Grain(options.seed)
        val lines = ArrayList<String>(rows + 2)

        lines += frameLine(optics, columns, options, top = true)

        for (row in 0 until rows) {
            val y = if (rows == 1) 0.0 else row.toDouble() / (rows - 1)
            val builder = StringBuilder()
            if (options.color) builder.append(ESC).append("38;2;").append(optics.edge.red)
                .append(';').append(optics.edge.green).append(';').append(optics.edge.blue).append('m')
            builder.append(if (options.color) "┃" else "|")
            for (column in 0 until columns) {
                val x = if (columns == 1) 0.0 else column.toDouble() / (columns - 1)
                var colour = optics.shadeAt(x, y)
                val speck = grain.chance(optics.speckle)
                if (speck) colour = colour.scale(0.93)
                if (grain.chance(optics.fleck)) colour = colour.mix(Rgb(120, 104, 82), 0.55)
                val curl = curlShade(x, y, sheet.stock.finish)
                if (curl != null) colour = colour.mix(curl.first, curl.second)
                builder.append(background(colour, options.color)).append(' ')
            }
            if (options.color) builder.append(RESET)
            builder.append(if (options.color) "┃" else "|")
            lines += builder.toString()
        }

        lines += frameLine(optics, columns, options, top = false)
        return lines.joinToString("\n")
    }

    /**
     * Lower-right corner curls toward the viewer: a small triangle of shaded
     * cells with a lighter underside.
     */
    private fun curlShade(x: Double, y: Double, finish: PaperFinish): Pair<Rgb, Double>? {
        if (finish == PaperFinish.CARDBOARD) return null
        val size = 0.22
        if (x < 1.0 - size || y < 1.0 - size) return null
        val along = ((x - (1.0 - size)) / size + (y - (1.0 - size)) / size) / 2.0
        if (along <= 0.0) return null
        return Rgb(226, 222, 212) to (0.35 + 0.4 * along)
    }

    private fun frameLine(optics: PaperOptics, columns: Int, options: Options, top: Boolean): String {
        val glyph = when {
            top && options.color -> "╭" + "─".repeat(columns) + "╮"
            top -> "+" + "-".repeat(columns) + "+"
            options.color -> "╰" + "─".repeat(columns) + "╯"
            else -> "+" + "-".repeat(columns) + "+"
        }
        return if (options.color) {
            val edge = optics.edge
            ESC + "38;2;${edge.red};${edge.green};${edge.blue}m" + glyph + RESET
        } else {
            glyph
        }
    }

    /**
     * A two-tone shadow offset down and right of the sheet.
     */
    private fun dropShadow(sheet: PaperSheet, options: Options): String {
        val optics = PaperOptics(sheet.stock)
        val (columns, rows) = cellsFor(sheet, options.width, 60)
        val near = background(optics.dropShadow.scale(0.85), options.color)
        val far = background(optics.dropShadow, options.color)
        val lines = ArrayList<String>()
        repeat(rows / 3 + 1) { lines += " ".repeat(3) + near.repeat(2) + (if (options.color) RESET else "") }
        lines += " ".repeat(3) + far.repeat(columns + 1) + (if (options.color) RESET else "")
        return lines.joinToString("\n")
    }

    /**
     * Edge view of the ream: a stack of sheets seen from the side.
     */
    private fun stack(ream: PaperReam, options: Options): String {
        val sheet = ream.sheet
        val optics = PaperOptics(sheet.stock)
        val perSheet = max(
            1,
            (ream.sheetCount.toDouble() * sheet.thickness.value /
                (PaperSheet.SHEETS_PER_REAM * 1.0)).roundToInt().coerceIn(0, 3),
        )
        val lines = ArrayList<String>()
        val body = background(optics.shadeAt(0.4, 0.5), options.color)
        val layer = background(optics.edge, options.color)
        repeat(perSheet) { lines += "  " + layer + " ".repeat(options.width.coerceAtMost(40)) + (if (options.color) RESET else "") }
        lines += "  " + body + " ".repeat(options.width.coerceAtMost(40)) + (if (options.color) RESET else "")
        return lines.joinToString("\n")
    }

    private fun caption(sheet: PaperSheet, options: Options): String {
        val columns = options.width
        val label = " ${sheet.size.name} ${if (sheet.landscape) "landscape" else "portrait"} "
        val meta = " ${sheet.stock.basisWeight} · ${sheet.mass} · ${sheet.thickness} "
        return frameText(label, columns) + frameText(meta, columns, right = true)
    }

    private fun stackCaption(ream: PaperReam, options: Options): String {
        val columns = options.width
        val left = " ${ream.sheetCount} sheets "
        val right = " ${ream.totalMass} · ${ream.stackHeight} "
        return frameText(left, columns) + frameText(right, columns, right = true)
    }

    private fun frameText(text: String, width: Int, right: Boolean = false): String {
        val inner = max(width - 2, text.length)
        val padded = if (right) " ".repeat(max(inner - text.length, 0)) + text else text
        return "|$padded|\n"
    }

    private fun background(colour: Rgb, color: Boolean): String =
        if (color) colour.toAnsiBackground() else ""
}