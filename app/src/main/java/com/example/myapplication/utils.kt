package com.example.myapplication

fun <T> shiftLeft(list: List<T>, shift: Int): List<T> {
    val n = list.size
    var actualShift = shift % n
    if(actualShift < 0) {
        actualShift += n
    }
    val result = MutableList(n) { i ->
        list[(i + actualShift) % n]
    }
    return result
}

fun <T> shiftRight(list: List<T>, shift: Int): List<T> {
    return shiftLeft(list, -shift)
}

fun conjugation(list: List<Complex>): List<Complex> {
    return MutableList(list.size) { i ->
        list[i].conjugate()
    }
}

fun magnitude(list: List<Complex>) : List<Double> {
    return List(list.size) { i ->
        list[i].abs()
    }
}


fun main() {
    val originalList = listOf(1, 2, 3, 4, 5)
    val shiftedList = shiftRight(originalList, 2)
    println(shiftedList)
}