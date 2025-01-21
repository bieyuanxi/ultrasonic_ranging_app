package com.example.myapplication

import org.junit.Test

import org.junit.Assert.*

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.ceil
import kotlin.math.log2
/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        // 输入数据，这里使用一个实数数组作为示例
        val u = 2
        val q = 81
        val N = 81
        val input = generateZCSequence1(u, q, N).toTypedArray()
        for (e in input) {
            println(e)
        }
        val newLength = 1 shl ceil(log2(input.size.toDouble())).toInt()
        println(newLength)
        val paddedInput = Array(newLength) { Complex(0.0, 0.0) }
        for (i in input.indices) {
            paddedInput[i] = input[i]
        }
        // 创建一个 FastFourierTransformer 实例，使用标准的 DFT 归一化
        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        // 执行正向傅里叶变换
        val result = transformer.transform(paddedInput, TransformType.FORWARD)
        // 打印结果
        for (complex in result) {
            println(complex)
        }
    }
}

fun generateZCSequence1(u: Int, q: Int, N: Int): List<Complex> {
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