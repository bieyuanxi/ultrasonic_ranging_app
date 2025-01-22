package com.example.myapplication

import org.junit.Test


class DFTKtTest {

    @Test
    fun dft() {
        val inputData = arrayOf(
            Complex(1.0, 2.0),
            Complex(2.0, 3.0),
            Complex(3.0, 4.0),
            Complex(4.0, 5.0),
            Complex(5.0, 6.0),
            Complex(6.0, 7.0),
            Complex(7.0, 8.0),
        )
        val realResult = arrayOf(
            Complex(28.0, 35.0),
            Complex(-10.76782489, 3.76782489),
            Complex(-6.29115686, -0.70884314),
            Complex(-4.29885216, -2.70114784),
            Complex(-2.70114784, -4.29885216),
            Complex(-0.70884314, -6.29115686),
            Complex(3.76782489, -10.76782489),
        )
        val result = dft(inputData.toList())
        for (i in inputData.indices) {
            assert((realResult[i] - result[i]).abs() < 1e-8)
        }
    }

    @Test
    fun idft() {
        val inputData = arrayOf(
            Complex(1.0, 2.0),
            Complex(2.0, 3.0),
            Complex(3.0, 4.0),
            Complex(4.0, 5.0),
            Complex(5.0, 6.0),
            Complex(6.0, 7.0),
            Complex(7.0, 8.0),
        )
        val realResult = arrayOf(
            Complex(4.0, 5.0),
            Complex(0.5382607, -1.5382607),
            Complex(-0.10126331, -0.89873669),
            Complex(-0.38587826, -0.61412174),
            Complex(-0.61412174, -0.38587826),
            Complex(-0.89873669, -0.10126331),
                Complex(-1.5382607, 0.5382607),
        )
        val result = idft(inputData.toList())
        for (i in inputData.indices) {
            assert((realResult[i] - result[i]).abs() < 1e-8)
        }
    }

    @Test
    fun idft1() {
        val inputData = arrayOf(
            Complex(10.0, 0.0),
            Complex(-2.0, 2.0),
            Complex(-2.0, 0.0),
            Complex(-2.0, -2.0),
            Complex(-3.0, 6.0),
        )

        val realResult = arrayOf(
            Complex(0.2, 1.2),
            Complex(2.86392771, 1.00824529),
            Complex(3.41245421, -1.30047694),
            Complex(1.71115259, -1.53559104),
            Complex(1.81246549, 0.62782269),
        )
        val result = idft(inputData.toList())
        for (i in inputData.indices) {
            assert((realResult[i] - result[i]).abs() < 1e-8)
        }
    }

    @Test
    fun dft2idft2dft() {
        val inputData = arrayOf(
            Complex(5.0, 0.0),
            Complex(-4.0, 3.0),
            Complex(-3.0, 2.0),
            Complex(-4.0, -1.0),
            Complex(-3.0, 6.0),
        )

        val result = idft(dft(inputData.toList()))
        for (i in inputData.indices) {
            assert((inputData[i] - result[i]).abs() < 1e-10)
        }
    }
}