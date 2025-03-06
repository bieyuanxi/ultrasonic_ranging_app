package com.example.myapplication

import org.junit.Test
import kotlin.math.PI
import kotlin.math.absoluteValue


class UtilsKtTest {

    @Test
    fun shiftLeft() {
        val originalList = listOf(1, 2, 3, 4, 5)
        val shiftedList = shiftLeft(originalList, 2)

        assert(shiftedList == listOf(3, 4, 5, 1, 2))
    }

    @Test
    fun shiftRight() {
        val originalList = listOf(1, 2, 3, 4, 5)
        val shiftedList = shiftRight(originalList, 2)

        assert(shiftedList == listOf(4, 5, 1, 2, 3))
    }

    @Test
    fun testCalculatePhaseShift() {
        var complex = Complex(1.0, 1.0)
        assert((calculatePhaseShift(complex) - PI / 4).absoluteValue < 1e-10)

        complex = Complex(1.0, 0.0)
        assert((calculatePhaseShift(complex) - 0).absoluteValue < 1e-10)

        complex = Complex(0.0, 1.0)
        assert((calculatePhaseShift(complex) - PI / 2).absoluteValue < 1e-10)

        complex = Complex(-1.0, 1.0)
        assert((calculatePhaseShift(complex) - 3 * PI / 4).absoluteValue < 1e-10)

        complex = Complex(1.0, -1.0)
        assert((calculatePhaseShift(complex) - (-PI / 4)).absoluteValue < 1e-10)

        complex = Complex(0.0, -1.0)
        assert((calculatePhaseShift(complex) - (-PI / 2)).absoluteValue < 1e-10)

        complex = Complex(-1.0, -1.0)
        assert((calculatePhaseShift(complex) - (-3 * PI / 4)).absoluteValue < 1e-10)

        complex = Complex(-1.0, 0.0)
        assert((calculatePhaseShift(complex) - (-PI)).absoluteValue < 1e-10)

    }
}