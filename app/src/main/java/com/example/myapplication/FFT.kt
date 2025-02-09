package com.example.myapplication

import com.sun.jna.Structure

@Structure.FieldOrder("re", "im")
class Complex32 : Structure {
    @JvmField var re: Float = 0f
    @JvmField var im: Float = 0f

    constructor() : super()
    constructor(re: Float, im: Float) : super() {
        this.re = re
        this.im = im
    }

}



