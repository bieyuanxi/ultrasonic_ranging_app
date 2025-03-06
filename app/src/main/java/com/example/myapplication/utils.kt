package com.example.myapplication

import kotlin.math.PI
import kotlin.math.atan

fun <T> shiftLeft(list: List<T>, shift: Int): List<T> {
    val n = list.size
    var actualShift = shift % n
    if(actualShift < 0) {
        actualShift += n
    }
    val result = MutableList(n) { i ->
        list[(i + actualShift) % n]
    }
    return result
}

fun <T> shiftRight(list: List<T>, shift: Int): List<T> {
    return shiftLeft(list, -shift)
}

fun conjugation(list: List<Complex>): List<Complex> {
    return MutableList(list.size) { i ->
        list[i].conjugate()
    }
}

fun magnitude(list: List<Complex>) : List<Double> {
    return List(list.size) { i ->
        list[i].abs()
    }
}

fun signedMagnitude(list: List<Complex>) : List<Double> {
    return List(list.size) { i ->
        val abs = list[i].abs()
        if (list[i].real > 0) {
            abs
        }else {
            -abs
        }
    }
}

fun discreteImpulseTrain(Nzc: Int = 960, odd: Boolean = false): List<Int> {
    return List(Nzc) { i ->
        if (odd) {
            (i) % 2
        } else {
            (i + 1) % 2
        }
    }
}

// [-PI, PI)
fun calculatePhaseShift(complex: Complex): Double {
    val realPart = complex.real
    val imaginaryPart = complex.imag
    return when {
        realPart == 0.0 && imaginaryPart > 0 -> PI / 2
        realPart == 0.0 && imaginaryPart < 0 -> - PI / 2
        realPart > 0 -> atan(imaginaryPart / realPart)
        imaginaryPart <= 0 -> -PI + atan(imaginaryPart / realPart)
        imaginaryPart > 0 -> PI + atan(imaginaryPart / realPart)
        else -> 0.0
    }
}

fun main() {
    val originalList = listOf(1, 2, 3, 4, 5)
    val shiftedList = shiftRight(originalList, 2)
    println(shiftedList)
}