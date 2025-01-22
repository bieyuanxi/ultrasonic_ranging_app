package com.example.myapplication

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// time complexity: O(n^2)
fun dft(input: List<Complex>): List<Complex> {
    val n = input.size
    val output = Array(n) { Complex(0.0, 0.0) }
    for (k in 0 until n) {
        for (i in 0 until n) {
            val angle = -2 * PI * k * i / n
            val c = Complex(cos(angle), sin(angle))
            output[k] = output[k] + input[i] * c
        }
    }
    return output.toList()
}


// time complexity: O(n^2)
fun idft(input: List<Complex>): List<Complex> {
    val N = input.size
    val output = Array(N) { Complex(0.0, 0.0) }
    for (k in 0 until N) {
        for (i in 0 until N) {
            val angle = 2 * PI * k * i / N
            val c = Complex(cos(angle), sin(angle))
            output[k] = output[k] + input[i] * c
        }
        output[k] = output[k] / N
    }
    return output.toList()
}