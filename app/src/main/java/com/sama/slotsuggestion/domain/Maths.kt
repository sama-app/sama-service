package com.sama.slotsuggestion.domain

import kotlin.math.exp


fun sigmoid(x: Double, L: Double = 1.0, k: Double = -1.0): Double {
    return L / (1 + exp(k * x))
}