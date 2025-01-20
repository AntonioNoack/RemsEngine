package org.joml

interface Matrix<SelfType, ColType, RowType> {

    val numRows: Int // size x, second number in JOML, first number in maths
    val numCols: Int // size y, first number in JOML, second number in maths

    fun equals(other: SelfType?, threshold: Double): Boolean

    operator fun get(column: Int, row: Int): Double
    operator fun set(column: Int, row: Int, value: Double): SelfType

    fun getRow(row: Int, dst: RowType): RowType
    fun setRow(row: Int, src: RowType): SelfType
    fun getColumn(column: Int, dst: ColType): ColType
    fun setColumn(column: Int, src: ColType): SelfType
}