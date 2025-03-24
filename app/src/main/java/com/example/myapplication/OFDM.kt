package com.example.myapplication

import android.util.Log

fun modulate(N: Int = 960, f_c: Int = 19000, f_s: Int = 48000, ZC_hat: List<Complex>): List<Complex> {
    val Nzc = ZC_hat.size
    val h_zc = Nzc / 2
    val n_c = N * f_c / f_s
    val X = Array(N) { Complex(0.0f, 0.0f) }
    for(i in 0 until Nzc) {
        X[i + n_c - h_zc] = ZC_hat[i]
    }

    for(i in (N / 2 + 1) until N) {
        X[i] = X[N - i].conjugate()
    }

//    return idft(X.toList())
    return RustFFTWrapper.ifft(X.toList())
}

fun modulate(ZC_hat: List<Complex>, N: Int = 960, f_c: Int = 19000, f_s: Int = 48000, I: List<Int>): List<Complex> {
    val Nzc = ZC_hat.size
    val h_zc = Nzc / 2
    val n_c = N * f_c / f_s
    val X = Array(N) { Complex(0.0f, 0.0f) }
    for(i in 0 until Nzc) {
        X[i + n_c - h_zc] = ZC_hat[i]
    }

    for(i in 0 until Nzc) {
        X[n_c - h_zc + i] = X[n_c - h_zc + i] * I[i]
    }

    for(i in (N / 2 + 1) until N) {
        X[i] = X[N - i].conjugate()
    }

//    return idft(X.toList())
    return RustFFTWrapper.ifft(X.toList())
}


fun demodulate(y: List<Complex>, ZC_hat_prime: List<Complex>, N_prime: Int, f_c: Int = 19000, f_s: Int = 48000, I: List<Int>): List<Complex> {
    val N = y.size
    val n_c = N * f_c / f_s
    val N_zc = ZC_hat_prime.size
    val h_zc = N_zc / 2

    // perform N-point DFT
//    val Y = dft(y)
    val Y = RustFFTWrapper.fft(y).toMutableList()

    for(i in 0 until N_zc) {
        Y[n_c - h_zc + i] = Y[n_c - h_zc + i] * I[i]
    }

    // conjugate multiplication
    val CFR_hat = MutableList(N_zc) { Complex(0.0f, 0.0f) }
    for (i in 0 .. 2 * h_zc) {
        CFR_hat[i] = ZC_hat_prime[i] * Y[i + n_c - h_zc]
    }

    // Zero padding
    val CFR = MutableList(N_prime) { Complex(0.0f, 0.0f) }
    for (i in 0 .. h_zc) {
        CFR[i] = CFR_hat[i + h_zc]
    }
    for (i in 0 until h_zc) {
//        CFR[N_prime - 1 - i] = CFR_hat[i] // 论文算法2中的代码，可能有误
        CFR[N_prime - h_zc + i] = CFR_hat[i]    // 根据论文Proof部分推断，应该是做循环位移
    }
    // perform N'-point IDFT
//    return idft(CFR)
    return RustFFTWrapper.ifft(CFR)
}

fun demodulate(y: List<Complex>, ZC_hat_prime: List<Complex>, N_prime: Int, f_c: Int = 19000, f_s: Int = 48000): List<Complex> {
    val N = y.size
    val n_c = N * f_c / f_s
    val N_zc = ZC_hat_prime.size
    val h_zc = N_zc / 2

    // perform N-point DFT
//    val Y = dft(y)
    val Y = RustFFTWrapper.fft(y)

    // conjugate multiplication
    val CFR_hat = MutableList(N_zc) { Complex(0.0f, 0.0f) }
    for (i in 0 .. 2 * h_zc) {
        CFR_hat[i] = ZC_hat_prime[i] * Y[i + n_c - h_zc]
    }

    // Zero padding
    val CFR = MutableList(N_prime) { Complex(0.0f, 0.0f) }
    for (i in 0 .. h_zc) {
        CFR[i] = CFR_hat[i + h_zc]
    }
    for (i in 0 until h_zc) {
//        CFR[N_prime - 1 - i] = CFR_hat[i] // 论文算法2中的代码，可能有误
        CFR[N_prime - h_zc + i] = CFR_hat[i]    // 根据论文Proof部分推断，应该是做循环位移
    }
    // perform N'-point IDFT
//    return idft(CFR)
    return RustFFTWrapper.ifft(CFR)
}