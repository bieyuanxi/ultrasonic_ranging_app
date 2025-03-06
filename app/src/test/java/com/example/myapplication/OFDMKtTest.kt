package com.example.myapplication

import org.junit.Test
import kotlin.math.absoluteValue


class OFDMKtTest {

    @Test
    fun testModulate() {
        System.setProperty("jna.library.path", "native-libs");
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
    fun testDemodulate() {
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
        var x = modulate(N, f_c, f_s, ZC_hat)
        // ↓ sender
        // ~
        // ~
        // ~
        // ~
        // ↑ receiver
//        val N_prime = 960 * 4
        val N_prime = 960
        val y = x
        val ZC_hat_prime = conjugation(ZC_hat)
        val cir = demodulate(y, ZC_hat_prime, N_prime, f_c, f_s)
        val mag = magnitude(cir)

        assert((mag[0] - 6.83437500e+00).absoluteValue < 1e-8)
        assert((mag[1] - 6.75463369e+00).absoluteValue < 1e-8)
        assert((mag[2] - 6.51875405e+00).absoluteValue < 1e-8)
        assert((mag[3] - 6.13660202e+00).absoluteValue < 1e-8)

        assert((mag[N_prime - 4] - 5.62407373).absoluteValue < 1e-8)
        assert((mag[N_prime - 3] - 6.13660202).absoluteValue < 1e-8)
        assert((mag[N_prime - 2] - 6.51875405).absoluteValue < 1e-8)
        assert((mag[N_prime - 1] - 6.75463369).absoluteValue < 1e-8)


        assert((cir[N_prime - 20] - Complex(-1.0726589, 2.54421002e-15)).abs() < 1e-8)
        assert((cir[N_prime - 19] - Complex(-1.28723903, 2.32039842e-15)).abs() < 1e-8)

        println(mag)
    }

    @Test
    fun testDemodulate1() {
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
        var x = modulate(N, f_c, f_s, ZC_hat)
        // ↓ sender
        // ~
        // ~
        x = shiftRight(x, 100)  // !!!!!!!!!!!!!!
        // ~
        // ~
        // ↑ receiver
//        val N_prime = 960 * 4
        val N_prime = 960
        val y = x
        val ZC_hat_prime = conjugation(ZC_hat)
        val cir = demodulate(y, ZC_hat_prime, N_prime)
        val mag = magnitude(cir)

        assert((mag[99] - 6.754633694060812).absoluteValue < 1e-10)
        assert((mag[100] - 6.834375).absoluteValue < 1e-10)
        assert((mag[101] - 6.754633694060812).absoluteValue < 1e-10)


//        println(mag)
    }

    @Test
    fun testDemodulate3() {
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
        var x = modulate(N, f_c, f_s, ZC_hat)
        // ↓ sender
        // ~
        // ~
        x = shiftRight(x, 0)    // !!!!!!!!! offset 0
        // ~
        // ~
        // ↑ receiver
        val y = x
        val ZC_hat_prime = conjugation(ZC_hat)

        val Y = dft(y)

        // conjugate multiplication
        val CFR_hat = MutableList(2 * h_zc + 1) { Complex(0.0, 0.0) }
        for (i in 0 .. 2 * h_zc) {
            CFR_hat[i] = ZC_hat_prime[i] * Y[i + n_c - h_zc]
        }

        CFR_hat.forEachIndexed { index, complex ->
            assert((complex.real - Nzc).absoluteValue < 1e-10)
            assert(complex.imag.absoluteValue < 1e-10)
        }
    }

    @Test
    // 根据论文Proof部分得到的demodulate代码
    fun testDemodulate4() {
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val N = 960     // frame length
        val f_c = 19000 // carrier frequency
        val f_s = 48000 // sampling frequency
        val n_c = N * f_c / f_s

        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)

        val ZC_hat = shiftRight(ZC, h_zc)
        val ZC_hat_prime = conjugation(ZC_hat)

        val x = modulate(N, f_c, f_s, ZC_hat)

        // ↓ sender
        // ~
        // ~
        val delay = 233
        val t = delay.toDouble() / f_s
        val y = shiftRight(x, delay)    // !!!!!!!!! delay
        // ~
        // ~
        // ↑ receiver

        val Y = dft(y)

        // conjugate multiplication
        val CFR_hat = MutableList(2 * h_zc + 1) { Complex(0.0, 0.0) }
        for (i in 0 .. 2 * h_zc) {
            CFR_hat[i] = ZC_hat_prime[i] * Y[i + n_c - h_zc]
        }


        // Zero padding
        val N_prime = N
        val CFR = MutableList(N_prime) { Complex(0.0, 0.0) }
        for (i in 0 .. h_zc) {
            CFR[i] = CFR_hat[i + h_zc]
        }

        for (i in 0 until h_zc) {
//            CFR[N_prime - 1 - i] = CFR_hat[h_zc - 1 - i]
            CFR[N_prime - h_zc + i] = CFR_hat[i]
        }
//        println(CFR)
        // perform N'-point IDFT
        val cir = idft(CFR)

        val mag = magnitude(cir)
        //        println(cir)

        val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
        val maxIndex = maxIndexedValue?.index
        assert(maxIndex == delay)

    }

    @Test
    fun testDemodulate5() {
        System.setProperty("jna.library.path", "native-libs");
        val u = 1
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val N = 960     // frame length
        val f_c = 19000 // carrier frequency
        val f_s = 48000 // sampling frequency
        val n_c = N * f_c / f_s
        val Nprime = 960

        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)

        val ZC_hat = shiftRight(ZC, h_zc)
        val ZC_hat_prime = conjugation(ZC_hat)

        val oddDiscreteImpulseTrain =  discreteImpulseTrain(Nzc, true)
        val evenDiscreteImpulseTrain =  discreteImpulseTrain(Nzc, false)

        val x = modulate(ZC_hat, N, f_c, f_s, oddDiscreteImpulseTrain)

        // ↓ sender
        // ~
        // ~
        val delay = 233
        val t = delay.toDouble() / f_s
        val y = shiftRight(x, delay)    // !!!!!!!!! delay
        // ~
        // ~
        // ↑ receiver

        val cir = demodulate(y, ZC_hat_prime, Nprime, I = oddDiscreteImpulseTrain)
        val cir1 = demodulate(y, ZC_hat_prime, Nprime, I = evenDiscreteImpulseTrain)


        val mag = magnitude(cir)
        val mag1 = magnitude(cir1)
        
        println(cir)

        val maxIndexedValue = mag.withIndex().maxByOrNull { it.value }
        val maxIndex = maxIndexedValue?.index
        println(maxIndex)
        println(mag[maxIndex!!])
        val peerNext = maxIndex + Nprime / 2
        println("${peerNext}: ${mag[peerNext]}")
        assert(maxIndex == delay)
        assert(mag[maxIndex] == mag[peerNext])

        val maxIndexedValue1 = mag1.withIndex().maxByOrNull { it.value }
        val maxIndex1 = maxIndexedValue1?.index
        println(maxIndex1)
        println(mag1[maxIndex1!!])

        assert(mag1[maxIndex1].absoluteValue < 1e-6)
    }
}