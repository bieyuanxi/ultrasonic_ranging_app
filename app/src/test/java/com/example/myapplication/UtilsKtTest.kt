package com.example.myapplication

import org.junit.Test


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
}