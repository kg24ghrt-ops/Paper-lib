package dev.paperlib.cli

import dev.paperlib.PaperReam
import dev.paperlib.PaperSheet
import dev.paperlib.PaperSize
import dev.paperlib.PaperStock
import dev.paperlib.render.SheetRenderer

private const val USAGE = """
paper-lib — measure and draw a sheet of paper

usage:
  paperlib [--size A4] [--gsm 80] [--stock copy] [--landscape]
           [--count 500] [--kg 2.5] [--plain] [--width 44] [--help]

options:
  --size, -s      trim size: A0..A5, LETTER, LEGAL        (default A4)
  --gsm, -g       basis weight in g/m²                     (default 80)
  --stock, -t     stock preset: copy, premium, offset,
                  newsprint, recycled, board               (default copy)
  --landscape, -l draw and measure in landscape
  --count, -c     sheets in the ream                        (default 500)
  --kg, -k        build a ream of roughly this mass instead
  --plain         disable ANSI colour
  --width         render width in terminal cells            (default 44)
  --help, -h      show this message

examples:
  paperlib
  paperlib --size A3 --gsm 250
  paperlib --stock newsprint --landscape --kg 3
"""

data class Options(
    val size: PaperSize = PaperSize.A4,
    val stock: PaperStock = PaperStock.COPY_80,
    val landscape: Boolean = false,
    val count: Int = PaperSheet.SHEETS_PER_REAM,
    val kg: Double? = null,
    val color: Boolean = true,
    val width: Int = 44,
)

sealed interface Parsed {
    data class Ok(val options: Options) : Parsed
    data object Help : Parsed
    data class Error(val message: String) : Parsed
}

fun parseArgs(args: Array<String>): Parsed {
    var size = PaperSize.A4
    var stock = PaperStock.COPY_80
    var gsm: Double? = null
    var landscape = false
    var count = PaperSheet.SHEETS_PER_REAM
    var kg: Double? = null
    var color = true
    var width = 44

    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--help", "-h" -> return Parsed.Help
            "--landscape", "-l" -> landscape = true
            "--plain", "--no-color" -> color = false
            "--size", "-s", "--stock", "-t", "--gsm", "-g", "--count", "-c", "--kg", "-k", "--width" -> {
                val value = args.getOrNull(index + 1)
                    ?: return Parsed.Error("$arg needs a value")
                index++
                when (arg) {
                    "--size", "-s" -> size = PaperSize.fromNameOrNull(value)
                        ?: return Parsed.Error("unknown size '$value', try A4")

                    "--stock", "-t" -> stock = stockFromName(value)
                        ?: return Parsed.Error("unknown stock '$value', try copy")

                    "--gsm", "-g" -> gsm = value.toDoubleOrNull()
                        ?: return Parsed.Error("basis weight must be a number, got '$value'")

                    "--count", "-c" -> count = value.toIntOrNull()?.takeIf { it > 0 }
                        ?: return Parsed.Error("count must be a positive integer, got '$value'")

                    "--kg", "-k" -> kg = value.toDoubleOrNull()?.takeIf { it > 0.0 }
                        ?: return Parsed.Error("mass must be a positive number, got '$value'")

                    else -> width = value.toIntOrNull()?.takeIf { it >= 20 }
                        ?: return Parsed.Error("width must be at least 20 cells, got '$value'")
                }
            }

            else -> return Parsed.Error("unknown argument '$arg', try --help")
        }
        index++
    }

    gsm?.let { stock = PaperStock.nearestBasisWeight(it) }
    return Parsed.Ok(Options(size, stock, landscape, count, kg, color, width))
}

private fun stockFromName(name: String): PaperStock? = when (name.lowercase()) {
    "copy", "80", "80gsm" -> PaperStock.COPY_80
    "premium", "120", "120gsm" -> PaperStock.PREMIUM_120
    "offset", "90", "90gsm" -> PaperStock.OFFSET_90
    "newsprint", "45", "45gsm" -> PaperStock.NEWSPRINT_45
    "recycled", "100", "100gsm" -> PaperStock.RECYCLED_100
    "board", "250", "250gsm" -> PaperStock.BOARD_250
    else -> null
}

fun report(sheet: PaperSheet, ream: PaperReam): String = buildString {
    appendLine("sheet      ${sheet.describe()}")
    appendLine("trim       ${sheet.width} x ${sheet.height}  (${sheet.size.areaSquareMetres * 1_000_000.0} mm², ratio ${"%.4f".format(sheet.size.aspectRatio)})")
    appendLine("basis      ${sheet.stock.basisWeight}   density ${sheet.stock.density}   brightness ${sheet.stock.brightness} ISO")
    appendLine("caliper    ${sheet.thickness}  (${"%.3f".format(sheet.thickness.toMillimetres())} mm)")
    appendLine("mass       ${sheet.mass} per sheet")
    appendLine("ream       ${ream.sheetCount} sheets = ${ream.totalMass}  (${"%.2f".format(ream.stackHeight.toMillimetres())} mm tall)")
    appendLine("area       ${"%.3f".format(ream.totalArea)} m² in the ream")
}

fun main(args: Array<String>) {
    when (val parsed = parseArgs(args)) {
        is Parsed.Help -> println(USAGE.trim())
        is Parsed.Error -> {
            System.err.println(parsed.message)
            System.err.println(USAGE.trim())
            kotlin.system.exitProcess(2)
        }

        is Parsed.Ok -> {
            val options = parsed.options
            val sheet = PaperSheet(options.size, options.stock, options.landscape)
            val ream = options.kg?.let { PaperReam.forKilograms(sheet, it) }
                ?: PaperReam(sheet, options.count)
            val renderOptions = SheetRenderer.Options(
                width = options.width,
                color = options.color,
            )
            println(report(sheet, ream))
            println()
            println(SheetRenderer.render(sheet, renderOptions))
            println()
            println(SheetRenderer.render(ream, renderOptions))
        }
    }
}