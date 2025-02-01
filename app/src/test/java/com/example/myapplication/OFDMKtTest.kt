package com.example.myapplication

import org.junit.Test
import kotlin.math.absoluteValue


class OFDMKtTest {

    @Test
    fun testModulate() {
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)
        val ZC_hat = shiftRight(ZC, h_zc)

        val N = 960     // frame length
        val f_c = 19000 // carrier frequency
        val f_s = 48000 // sampling frequency
        val n_c = N * f_c / f_s
        val x = modulate(N, f_c, f_s, ZC_hat).toMutableList()
        // 结果的每一项元素应该是实数
        x.forEach { v -> assert(v.imag.absoluteValue < 1e-10) }

        assert((x[0] - Complex(1.68750000e-01, 0.0)).abs() < 1e-8)
        assert((x[1] - Complex(-1.36503503e-01, 0.0)).abs() < 1e-8)
        assert((x[2] - Complex(5.15869737e-02, 0.0)).abs() < 1e-8)

        assert((x[100] - Complex(0.15118087042021186, 0.0)).abs() < 1e-8)
        assert((x[300] - Complex(-0.13730117892259971, 0.0)).abs() < 1e-8)
        assert((x[400] - Complex(0.1874448788933975, 0.0)).abs() < 1e-8)
        assert((x[500] - Complex(0.04852759039416822, 0.0)).abs() < 1e-8)

        assert((x[N - 1] - Complex(-1.36560095e-01, 0.0)).abs() < 1e-8)
        assert((x[N - 2] - Complex(5.19453293e-02, 0.0)).abs() < 1e-8)
        assert((x[N - 3] - Complex(5.33523356e-02, 0.0)).abs() < 1e-8)

        x.forEachIndexed { index, complex -> x[index] = complex * 10000 }
        val X = dft(x)
        println(X[0])
//        assert((X[0] - Complex(4.440892098500626e-16, 0.0)).abs() < 1e-16)
    }

    @Test
    fun testModulate1() {
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)
        val ZC_hat = shiftRight(ZC, h_zc)

        val N = 960     // frame length
        val f_c = 19000 // carrier frequency
        val f_s = 48000 // sampling frequency
        val n_c = N * f_c / f_s
        var x = modulate(N, f_c, f_s, ZC_hat).toMutableList()
        val X = dft(x)
//        println(X[0])
//        x = shiftRight(x, 200).toMutableList()
        val audioData = x.map { it.real.toFloat() }.toFloatArray()
        val result = dft(audioData.toList().map {Complex(it.toDouble(), 0.0) })


//        println(audioData)
    }

    @Test
    fun demodulate() {
    }
}