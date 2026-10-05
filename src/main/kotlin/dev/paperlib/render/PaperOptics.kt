package dev.paperlib.render

import dev.paperlib.PaperFinish
import dev.paperlib.PaperStock

/**
 * Deterministic linear congruential generator, so a given seed always renders
 * the same sheet. Keeps renderer output testable.
 */
internal class Grain(seed: Long) {
    private var state = if (seed == 0L) 0x9E3779B97F4A7C15uL.toLong() else seed

    fun nextDouble(): Double {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 33).toInt() and 0x7FFFFFFF
        return bits.toDouble() / 0x7FFFFFFF.toDouble()
    }

    fun chance(probability: Double): Boolean = nextDouble() < probability
}

internal data class Rgb(val red: Int, val green: Int, val blue: Int) {
    fun mix(other: Rgb, amount: Double): Rgb = Rgb(
        red = lerp(red, other.red, amount),
        green = lerp(green, other.green, amount),
        blue = lerp(blue, other.blue, amount),
    )

    fun scale(factor: Double): Rgb = Rgb(
        red = clamp(red * factor),
        green = clamp(green * factor),
        blue = clamp(blue * factor),
    )

    companion object {
        private fun lerp(from: Int, to: Int, amount: Double): Int =
            (from + (to - from) * amount).coerceIn(0, 255)

        private fun clamp(channel: Double): Int = channel.toInt().coerceIn(0, 255)
    }

    fun toAnsiBackground(): String = "\u001B[48;2;$red;$green;${blue}m"
}

/**
 * Turns a stock's optical properties into colours.
 *
 * Lower ISO brightness and rougher finishes spread more light back, so the
 * drawn sheet is flatter and duller; smooth bright stock gets a tighter,
 * brighter highlight.
 */
internal class PaperOptics(private val stock: PaperStock) {
    private val base: Rgb = run {
        val dull = 1.0 - stock.brightness / 100.0
        Rgb(252, 251, 247).mix(Rgb(214, 208, 194), dull * 0.55)
    }

    private val shadow: Rgb = base
        .mix(Rgb(38, 40, 48), 0.35 + 0.25 * stock.matteFactor)
        .scale(1.0 - 0.08 * (1.0 - stock.brightness / 100.0))

    private val highlight: Rgb = base.mix(Rgb(255, 255, 255), 0.55 - 0.3 * stock.matteFactor)

    /** Cast shadow on the surface behind the sheet, tinted by ambient light. */
    val dropShadow: Rgb = Rgb(46, 48, 56).mix(base, 0.25 * stock.matteFactor)

    /**
     * Colour at normalised position (x, y) on the sheet face for light coming
     * from the top left, with a soft falloff into the far corner.
     */
    fun shadeAt(x: Double, y: Double): Rgb {
        val across = x.coerceIn(0.0, 1.0)
        val down = y.coerceIn(0.0, 1.0)
        val lightTerm = (1.0 - across) * 0.65 + (1.0 - down) * 0.35
        val corner = 1.0 - 0.18 * kotlin.math.hypot(across - 0.12, down - 0.08)
        val level = (lightTerm * corner).coerceIn(0.0, 1.0)
        return shadow.mix(highlight, level)
    }

    /** Speckle strength used by rough finishes. */
    val speckle: Double
        get() = when (stock.finish) {
            PaperFinish.SMOOTH -> 0.0
            PaperFinish.LINEN -> 0.05
            PaperFinish.LAID -> 0.06
            PaperFinish.NEWSPRINT -> 0.09
            PaperFinish.RECYCLED -> 0.10
            PaperFinish.CARDBOARD -> 0.05
        }

    /** Fibre flecks visible in recycled stock. */
    val fleck: Double
        get() = if (stock.finish == PaperFinish.RECYCLED) 0.012 else 0.0

    val edge: Rgb get() = base.scale(0.82)
}