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
    return curve(outsideValue, insideValue, vectorSize, start, end, 0 to 0)
    { throw UnsupportedOperationException() }
}

fun linearCurve(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, curveRange: Pair<Int, Int>
): Vector {
    return curve(outsideValue, insideValue, vectorSize, start, end, curveRange)
    { x -> x }
}

fun parabolicCurve(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, curveRange: Pair<Int, Int>
): Vector {
    return curve(outsideValue, insideValue, vectorSize, start, end, curveRange)
    { x -> x * x }
}

fun sigmoidCurve(
    outsideValue: Double, insideValue: Double, vectorSize: Int, start: Int, end: Int, curveRange: Pair<Int, Int>
): Vector {
    return curve(outsideValue, insideValue, vectorSize, start, end, curveRange)
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
 * @param curveRange range for the [start] and [end] to which the [curveFunction] will be applied. Examples:
 * * [-2 to 0] will apply to [start - 2 to start] and [end to end + 2];
 * * [-1 to 1] will apply to [start - 1 to start + 1] and [end -1 to end + 1]
 * * [-1 to 0] or [0 to 0] is equivalent to not having a curve
 * @param curveFunction function applied to the vector indices defined by the [curveRange]
 * @return a new [Vector]
 */
fun curve(
    outsideValue: Double,
    insideValue: Double,
    vectorSize: Int,
    start: Int,
    end: Int,
    curveRange: Pair<Int, Int>,
    curveFunction: (x: Double) -> Double
): Vector {
    return curve(
        outsideValue,
        insideValue,
        vectorSize,
        start,
        end,
        curveRange,
        curveFunction,
        curveRange,
        curveFunction
    )
}

/**
 * Create a [Vector] with a custom function applied to the vector indices in the range of transitioning between values
 * @param outsideValue value applied to the outside of the range
 * @param insideValue value applied to the inside of the range
 * @param vectorSize size of the vector
 * @param start index from which [insideValue] is applied from
 * @param end index to which [insideValue] is applied to
 * @param startCurveRange range for the [start] and [end] to which the [startCurveFunction] will be applied. Examples:
 * * [-2 to 0] will apply to [start - 2 to start]
 * * [-1 to 1] will apply to [start - 1 to start + 1]
 * * [-1 to 0] or [0 to 0] is equivalent to not having a curve
 * @param startCurveFunction function applied to the vector indices defined by the [startCurveRange]
 * @param endCurveRange range for the [start] and [end] to which the [endCurveFunction] will be applied. Examples:
 * * [-2 to 0] will apply to [end to end + 2];
 * * [-1 to 1] will apply to [end -1 to end + 1]
 * * [-1 to 0] or [0 to 0] is equivalent to not having a curve
 * @param endCurveFunction function applied to the vector indices defined by the [endCurveRange]
 * @return a new [Vector]
 */
fun curve(
    outsideValue: Double,
    insideValue: Double,
    vectorSize: Int,
    start: Int,
    end: Int,
    startCurveRange: Pair<Int, Int>,
    startCurveFunction: (x: Double) -> Double,
    endCurveRange: Pair<Int, Int>,
    endCurveFunction: (x: Double) -> Double
): Vector {
    check(startCurveRange.first <= 0 && startCurveRange.second >= 0) { "Invalid curve range" }
    check(endCurveRange.first <= 0 && endCurveRange.second >= 0) { "Invalid curve range" }
    check(end > start) { "End index must be greater than the start index" }
    check(end <= vectorSize) { "End index must be smaller than the vector size" }

    val startCurveLength = abs(startCurveRange.first) + startCurveRange.second.toDouble()
    val startCurveStart = start + startCurveRange.first
    val startCurveEnd = start + startCurveRange.second
    check(startCurveStart >= 0 && startCurveEnd <= vectorSize) { "Invalid start curve range" }

    val endCurveLength = abs(endCurveRange.first) + endCurveRange.second.toDouble()
    val endCurveStart = end - endCurveRange.second
    val endCurveEnd = end - endCurveRange.first
    check(endCurveStart >= 0 && endCurveEnd <= vectorSize) { "Invalid end curve range" }

    val valueRange = outsideValue - insideValue
    return ones(vectorSize)
        .mapIndexed { idx, _ ->
            if (idx < startCurveStart || idx >= endCurveEnd) {
                outsideValue
            } else if (idx in startCurveStart until startCurveEnd) {
                val idxInCurve = idx - startCurveStart
                outsideValue - valueRange * startCurveFunction.invoke(idxInCurve / startCurveLength)
            } else if (idx in endCurveStart until endCurveEnd) {
                val idxInCurve = idx - endCurveStart + 1
                outsideValue - valueRange * endCurveFunction.invoke(1 - idxInCurve / endCurveLength)
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
    return multiply(others.reduce { acc, other -> acc.multiply(other) })
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
    return add(others.reduce { acc, other -> acc.add(other) })
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
