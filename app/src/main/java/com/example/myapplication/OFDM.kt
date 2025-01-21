package com.example.myapplication

fun modulate(N: Int = 960, f_c: Int = 19000, f_s: Int = 48000, ZC_hat: List<Complex>): List<Complex> {
    val Nzc = ZC_hat.size
    val h_zc = Nzc / 2
    val n_c = N * f_c / f_s
    val X = Array(N) { Complex(0.0, 0.0) }
    for(i in 0 until Nzc) {
        X[i + n_c - h_zc] = ZC_hat[i]
    }

    for(i in (N / 2 + 1) until N) {
        X[i] = X[N - i].conjugate()
    }

    return idft(X.toList())
}


fun demodulate() {

}