package com.example.myapplication

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface RustFFTLib : Library {
    companion object {
        val INSTANCE: RustFFTLib by lazy {
            Native.load("rust_fft_wrapper", RustFFTLib::class.java)
        }
    }
    fun fft_forward(input: Pointer, output: Pointer, n: Int, inverse: Boolean)

    fun add(a: Int, b: Int): Int
}