package org.joml

interface Matrix {

    val numRows: Int // size x, second number in JOML, first number in maths
    val numCols: Int // size y, first number in JOML, second number in maths

    fun equals1(other: Matrix, threshold: Double): Boolean

}