package com.sama.slotsuggestion.domain

import java.time.Duration
import java.time.LocalTime
import kotlin.math.ceil

/**
 * User contextual data and utility methods for dealing with heatmap weights, in particular
 * [Vector] generation
 */
class WeightContext(val intervalMinutes: Int, val days: Int) {
    val singleDayVectorSize = 24 * 60 / intervalMinutes
    val multiDayVectorSize = singleDayVectorSize * days

    fun zeroes(): Vector {
        return zeroes(singleDayVectorSize)
    }

    fun multiDayZeroes(): Vector {
        return zeroes(multiDayVectorSize)
    }

    fun ones(): Vector {
        return ones(singleDayVectorSize)
    }

    fun multiDayOnes(): Vector {
        return zeroes(multiDayVectorSize)
    }

    fun line(value: Double): Vector {
        return Vector(singleDayVectorSize) { value }
    }

    fun cliff(start: LocalTime, end: LocalTime, outsideValue: Double, insideValue: Double): Vector {
        return curve(outsideValue, insideValue, singleDayVectorSize, timeToIndex(start), timeToIndex(end), 0 to 0)
        { throw UnsupportedOperationException() }
    }

    fun linearCurve(
        start: LocalTime, end: LocalTime, outsideValue: Double, insideValue: Double, curveRange: Pair<Int, Int>
    ): Vector {
        return linearCurve(outsideValue, insideValue, singleDayVectorSize, timeToIndex(start), timeToIndex(end), curveRange)
    }

    fun timeToIndex(time: LocalTime): Int {
        return ceil((time.hour * 60 + time.minute).toDouble() / intervalMinutes).toInt()
    }

    fun indexToDurationOffset(index: Int): Duration {
        return Duration.ofMinutes((index * intervalMinutes).toLong())
    }

    fun durationToOffset(duration: Duration): Int {
        return ceil(duration.toMinutes().toDouble() / intervalMinutes).toInt()
    }

    fun minutesToSlotOffset(minutes: Int): Int {
        return ceil(minutes.toDouble() / intervalMinutes).toInt()
    }

    fun daysToSlotOffset(days: Int): Int {
        return this.days * singleDayVectorSize
    }
}