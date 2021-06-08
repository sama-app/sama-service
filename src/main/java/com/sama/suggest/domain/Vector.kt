package com.sama.suggest.domain

import com.sama.common.mapIndexedMutating
import kotlin.math.abs
import kotlin.math.exp

typealias Vector = MutableList<Double>

// TODO: this is bad
fun Vector.isOnes(): Boolean {
    return find { v -> v != 1.0 } != null
}

// TODO: this is bad
fun Vector.isZeroes(): Boolean {
    return find { v -> v != 0.0 } != null
}

fun Vector.copy(): Vector {
    return toMutableList()
}

fun zeroes(vectorSize: Int): Vector {
    return MutableList(vectorSize) { 0.0 }
}

fun ones(vectorSize: Int): Vector {
    return MutableList(vectorSize) { 1.0 }
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

private fun sigmoid(x: Double, sharpness: Double = 4.0): Double {
    return (1 / (1 + exp(-sharpness * x))) - 0.5
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
    check(outsideValue in 0.0..1.0 && insideValue in 0.0..1.0)

    val slopeLength = abs(slopeRange.first) + slopeRange.second.toDouble()
    val startSlopeStart = start + slopeRange.first
    val startSlopeEnd = start + slopeRange.second
    check(startSlopeStart >= 0 && startSlopeEnd <= vectorSize) { "Invalid start slope range" }

    val endSlopeStart = end - slopeRange.second
    val endSlopeEnd = end - slopeRange.first
    check(endSlopeStart >= 0 && endSlopeEnd <= vectorSize) { "Invalid end slope range" }

    val valueRange = outsideValue - insideValue
    return ones(vectorSize)
        .mapIndexedMutating { idx, _ ->
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
    // TODO check if other is ones and do nothing
    mapIndexedMutating { idx, value -> value * other[idx] }
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
    // TODO check if other is ones and do nothing
    mapIndexedMutating { idx, value -> value / other[idx] }
    return this
}

fun Vector.add(other: Vector): Vector {
    check(size == other.size) { "Vectors must be the same size" }
    // TODO check if other is zeroes and do nothing
    mapIndexedMutating { idx, value -> value + other[idx] }
    return this
}

fun Vector.add(others: Collection<Vector>): Vector {
    if (others.isEmpty()) {
        return this
    }
    return add(others.reduce { acc, mask -> acc.add(mask) })
}

fun Vector.scalarMultiply(value: Double): Vector {
    check(value >= 0) { "Scalar value must be greater than or equal to 0" }
    if (value == 1.0) {
        return this
    }
    replaceAll { it * value }
    return this
}

fun Vector.scalarDivide(value: Double): Vector {
    check(value > 0) { "Scalar value must be greater than 0" }
    if (value == 1.0) {
        return this
    }
    replaceAll { it / value }
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
    return windowed(slotSize)
        .map { it.reduce { acc, d -> acc * d } }
        .toMutableList()
}

typealias Matrix = MutableList<Vector>

@JvmName("matrixMultiply")
fun Matrix.multiply(other: Vector): Matrix {
    check(size == other.size) { "Matrix size must be the same as the input Vector" }
    mapIndexedMutating { idx, value -> value.scalarMultiply(other[idx]) }
    return this
}