package dev.paperlib

import kotlin.jvm.JvmInline

/**
 * Basis weight: mass of one square metre of the stock, in grams.
 */
@JvmInline
value class GramsPerSquareMeter(val value: Double) {
    init {
        require(value > 0.0) { "basis weight must be positive, was $value" }
    }

    override fun toString(): String = "%.0f g/m²".format(value)
}

/**
 * Sheet thickness in micrometres, derived from basis weight and material density.
 */
@JvmInline
value class Micrometres(val value: Double) {
    init {
        require(value >= 0.0) { "thickness cannot be negative, was $value" }
    }

    fun toMillimetres(): Double = value / 1000.0

    override fun toString(): String =
        if (value < 100.0) "%.1f µm".format(value) else "%.0f µm".format(value)
}

/**
 * Material density in grams per cubic centimetre.
 */
@JvmInline
value class GramsPerCubicCentimetre(val value: Double) {
    init {
        require(value > 0.0) { "density must be positive, was $value" }
    }

    override fun toString(): String = "%.3f g/cm³".format(value)
}

/**
 * Linear mass in grams.
 */
@JvmInline
value class Grams(val value: Double) {
    init {
        require(value >= 0.0) { "mass cannot be negative, was $value" }
    }

    fun toKilograms(): Double = value / 1000.0

    override fun toString(): String =
        if (value < 1000.0) "%.2f g".format(value) else "%.2f kg".format(toKilograms())
}

/**
 * Sheet length in millimetres.
 */
@JvmInline
value class Millimetres(val value: Double) {
    init {
        require(value > 0.0) { "length must be positive, was $value" }
    }

    fun toInches(): Double = value / 25.4

    override fun toString(): String = "%.1f mm".format(value)
}