package com.example.myapplication

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// TODO：用Common Math里的Complex代替
data class Complex(val real: Double, val imag: Double) {
    operator fun plus(other: Complex): Complex {
        return Complex(real + other.real, imag + other.imag)
    }

    operator fun minus(other: Complex): Complex {
        return Complex(real - other.real, imag - other.imag)
    }

    operator fun times(other: Complex): Complex {
        val realPart = real * other.real - imag * other.imag
        val imagPart = real * other.imag + imag * other.real
        return Complex(realPart, imagPart)
    }

    operator fun times(other: Int): Complex {
        return Complex(real * other, imag * other)
    }

    operator fun div(divisor: Double): Complex {
        return Complex(real / divisor, imag / divisor)
    }

    operator fun div(divisor: Int): Complex {
        return Complex(real / divisor, imag / divisor)
    }

    fun conjugate(): Complex {
        return Complex(real, -imag)
    }

    fun abs(): Double {
        return kotlin.math.sqrt(real * real + imag * imag)
    }
}


fun generateZCSequence(u: Int, q: Int, N: Int): List<Complex> {
    assert(N <= q)
    val sequence = mutableListOf<Complex>()
    for (n in 0 until N) {
        // 计算 ZC 序列元素的公式 exp(-j * pi * u * n * (n + 1) / q)
        val phase = -PI * u * n * (n + 1) / q
        val realPart = cos(phase)
        val imagPart = sin(phase)
        sequence.add(Complex(realPart, imagPart))
    }
    return sequence
}

// 自相关运算
fun autoCorrelation(sequence: List<Complex>, k: Int): Complex {
    val n = sequence.size
    var result = Complex(0.0, 0.0)
    for (i in 0 until n) {
        val index = (i + k) % n
        result += sequence[i] * sequence[index].conjugate()
    }
    return result
}

// 互相关运算
fun crossCorrelation(sequence1: List<Complex>, sequence2: List<Complex>, k: Int): Complex {
    val N = sequence1.size
    var result = Complex(0.0, 0.0)
    for (n in 0 until N) {
        val index = (n + k) % N
        result += sequence1[n] * sequence2[index].conjugate()
    }
    return result
}