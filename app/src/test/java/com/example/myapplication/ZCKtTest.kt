package com.example.myapplication

import org.junit.Test

class ZCKtTest {
    @Test
    /// ZC 序列的幅度是恒定的
    fun test1() {
        val u = 2
        val q = 257
        val N = 128
        val zcSequence = generateZCSequence(u, q, N)
        for (element in zcSequence) {
            println("${element.real}+${element.imag}j")
            val mo_2 = element.real * element.real + element.imag * element.imag
            assert(kotlin.math.abs(element.abs() - 1.0) < 1e-10)
        }
    }


    @Test
    // ZC 序列具有良好的自相关特性，同一 ZC 序列的不同循环移位版本之间的自相关性接近零，
    // 仅在零移位（即完全相同）时，自相关性达到峰值
    // 自相关特性验证
    fun test2() {
        val u = 1
        val q = 257 // 素数
        val N = 257
        val zcSequence = generateZCSequence(u, q, N)
        // 检查 k = 0 时的自相关
        val autoCorrZero = autoCorrelation(zcSequence, 0)
        println("Auto-correlation at k = 0: ${autoCorrZero.real} + ${autoCorrZero.imag}j")
        assert((autoCorrZero.abs() - N) < 1e-10)
        assert(autoCorrZero.imag < 1e-10)
        // 检查 k = 1 时的自相关
        val autoCorrOne = autoCorrelation(zcSequence, 1)
        println("Auto-correlation at k = 1: ${autoCorrOne.real} + ${autoCorrOne.imag}j")
        assert(autoCorrOne.abs() < 1e-10)
        val autoCorrN = autoCorrelation(zcSequence, 10)
        println("Auto-correlation at k = N: ${autoCorrN.real} + ${autoCorrN.imag}j")
        assert(autoCorrN.abs() < 1e-10)
    }


    @Test
    // 低互相关特性
    fun test3() {
        // u1 u2互质
        val u1 = 2
        val u2 = 3
        val q = 257 // q为质数
        val N = 128
        val zcSequence1 = generateZCSequence(u1, q, N)
        val zcSequence2 = generateZCSequence(u2, q, N)
        // 检查 k = 0 时的互相关
        val crossCorrZero = crossCorrelation(zcSequence1, zcSequence2, 0)
        println("Cross-correlation at k = 0: ${crossCorrZero.real} + ${crossCorrZero.imag}j")
        // 检查 k = 1 时的互相关
        val crossCorrOne = crossCorrelation(zcSequence1, zcSequence2, 1)
        println("Cross-correlation at k = 1: ${crossCorrOne.real} + ${crossCorrOne.imag}j")
    }
}




