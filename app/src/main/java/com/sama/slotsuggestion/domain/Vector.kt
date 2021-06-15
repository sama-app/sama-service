package com.sama.slotsuggestion.domain

import com.sama.common.mapIndexed
import kotlin.math.abs
import kotlin.math.exp

typealias Vector = DoubleArray


fun line(vectorSize: Int, value: Double): Vector {
    return DoubleArray(vectorSize) { value }
}

fun zeroes(vectorSize: Int): Vector {
    return line(vectorSize, 0.0)
}

fun ones(vectorSize: Int): Vector {
    return line(vectorSize, 1.0)
}

fun cliff(outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int): Vector {
    return slope(outsideValue, insideValue, vectorSize, start, end, 0 to 0)
    { throw UnsupportedOperationException() }
}

fun linearSlope(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, slopeRange: Pair<Int, Int>
): Vector {
    return slope(outsideValue, insideValue, vectorSize, start, end, slopeRange)
    { x -> x }
}

fun parabolicSlope(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, slopeRange: Pair<Int, Int>
): Vector {
    return slope(outsideValue, insideValue, vectorSize, start, end, slopeRange)
    { x -> x * x }
}

fun sigmoidSlope(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, slopeRange: Pair<Int, Int>
): Vector {
    return slope(outsideValue, insideValue, vectorSize, start, end, slopeRange)
    { x -> 0.5 / sigmoid(1.0) * sigmoid(2 * x - 1) + 0.5 }
}

fun sigmoid(x: Double, sharpness: Double = 1.0): Double {
    return 1 / (1 + exp(-x))
}

/**
 * Create a [Vector] with a custom function applied to the vector indices in the range of transitioning between values
 * @param outsideValue value applied to the outside of the range
 * @param insideValue value applied to the inside of the range
 * @param vectorSize size of the vector
 * @param start index from which [insideValue] is applied from
 * @param end index to which [insideValue] is applied to
 * @param slopeRange range for the [start] and [end] to which the [slopeFunction] will be applied. Examples:
 * * [-2 to 0] will apply to [start - 2 to start] and [end to end + 2];
 * * [-1 to 1] will apply to [start - 1 to start + 1] and [end -1 to end + 1]
 * * [-1 to 0] or [0 to 0] is equivalent to not having a slope
 * @param slopeFunction function applied to the vector indices defined by the [slopeRange]
 * @return a new [Vector]
 */
fun slope(
    outsideValue: Double,
    insideValue: Double,
    vectorSize: Int,
    start: Int,
    end: Int,
    slopeRange: Pair<Int, Int>,
    slopeFunction: (x: Double) -> Double
): Vector {
    check(slopeRange.first <= 0 && slopeRange.second >= 0) { "Invalid slope range" }
    check(end > start) { "End index must be greater than the start index" }
    check(end <= vectorSize) { "End index must be smaller than the vector size" }

    val slopeLength = abs(slopeRange.first) + slopeRange.second.toDouble()
    val startSlopeStart = start + slopeRange.first
    val startSlopeEnd = start + slopeRange.second
    check(startSlopeStart >= 0 && startSlopeEnd <= vectorSize) { "Invalid start slope range" }

    val endSlopeStart = end - slopeRange.second
    val endSlopeEnd = end - slopeRange.first
    check(endSlopeStart >= 0 && endSlopeEnd <= vectorSize) { "Invalid end slope range" }

    val valueRange = outsideValue - insideValue
    return ones(vectorSize)
        .mapIndexed { idx, _ ->
            if (idx < startSlopeStart || idx >= endSlopeEnd) {
                outsideValue
            } else if (idx in startSlopeStart until startSlopeEnd) {
                val idxInSlope = idx - startSlopeStart
                outsideValue - valueRange * slopeFunction.invoke(idxInSlope / slopeLength)
            } else if (idx in endSlopeStart until endSlopeEnd) {
                val idxInSlope = idx - endSlopeStart + 1
                outsideValue - valueRange * slopeFunction.invoke(1 - idxInSlope / slopeLength)
            } else {
                insideValue
            }
        }
}

fun Vector.multiply(other: Vector): Vector {
    check(size == other.size) { "Vectors must be the same size" }
    mapIndexed { idx, value -> value * other[idx] }
    return this
}

fun Vector.multiply(others: Collection<Vector>): Vector {
    if (others.isEmpty()) {
        return this
    }
    return multiply(others.reduce { acc, mask -> acc.multiply(mask) })
}

fun Vector.divide(other: Vector): Vector {
    check(size == other.size) { "Vectors must be the same size" }
    mapIndexed { idx, value -> value / other[idx] }
    return this
}

fun Vector.add(other: Vector): Vector {
    check(size == other.size) { "Vectors must be the same size" }
    mapIndexed { idx, value -> value + other[idx] }
    return this
}

fun Vector.add(others: Collection<Vector>): Vector {
    if (others.isEmpty()) {
        return this
    }
    return add(others.reduce { acc, mask -> acc.add(mask) })
}

fun Vector.scalarMultiply(input: Double): Vector {
    check(input >= 0) { "Scalar value must be greater than or equal to 0" }
    if (input == 1.0) {
        return this
    }
    mapIndexed { _, value -> value * input }
    return this
}

fun Vector.scalarDivide(input: Double): Vector {
    check(input > 0) { "Scalar value must be greater than 0" }
    if (input == 1.0) {
        return this
    }
    mapIndexed { _, value -> value / input }
    return this
}

fun Vector.normalize(): Vector {
    val maxValue = this.maxOrNull()!!
    if (maxValue == 1.0) {
        return this
    }
    return scalarDivide(maxValue)
}

fun Vector.zipMultiplying(slotSize: Int): Vector {
    // not efficient as we're converting to List<List<Double>>
    // and then back to DoubleArray
    return asIterable().windowed(slotSize)
        .map { it.reduce { acc, d -> acc * d } }
        .toDoubleArray()
}
