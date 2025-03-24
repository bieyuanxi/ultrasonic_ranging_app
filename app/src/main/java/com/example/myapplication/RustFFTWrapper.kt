package com.example.myapplication

import com.sun.jna.Memory

object RustFFTWrapper {
    val lib = RustFFTLib.INSTANCE

    fun process(input: List<Complex>, inverse: Boolean, _output: List<Complex>? = null): List<Complex> {
        val inputSize = input.size
        val inputPtr = Memory(inputSize * 8L) // 每个Complex占8字节（2个float）
        input.forEachIndexed { i, c ->
            inputPtr.setFloat(i * 8L, c.real)
            inputPtr.setFloat(i * 8L + 4, c.imag)
        }
        val outputPtr = Memory(inputSize * 8L)

        lib.fft_forward(inputPtr, outputPtr, inputSize, inverse)

        val output = MutableList(inputSize){Complex(0.0f, 0.0f)}
        output.forEachIndexed { i, _ ->
            output[i].real = outputPtr.getFloat(i * 8L)
            output[i].imag = outputPtr.getFloat(i * 8L + 4)

        }
        inputPtr.close()
        outputPtr.close()
        return output
    }

    fun fft(input: List<Complex>, _output: List<Complex>? = null): List<Complex> {
        return this.process(input, false, _output)
    }

    fun ifft(input: List<Complex>, _output: List<Complex>? = null): List<Complex> {
        return this.process(input, true, _output)
    }
}