package com.example.myapplication

import org.junit.Test


class OFDMKtTest {

    @Test
    fun testModulate() {
        val u = 2
        val q = 81
        val Nzc = 81
        val h_zc = Nzc / 2
        val zc = generateZCSequence(u, q, Nzc)
        val ZC = dft(zc)
        val ZC_hat = shiftRight(zc, h_zc)

        val N = 960     // frame length
        val f_c = 19000 // carrier frequency
        val f_s = 48000 // sampling frequency
        val n_c = N * f_c / f_s
        val x = modulate(N, f_c, f_s, ZC_hat)
        TODO()
    }

    @Test
    fun demodulate() {
    }
}