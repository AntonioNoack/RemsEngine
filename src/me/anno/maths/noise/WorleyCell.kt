package me.anno.maths.noise

data class WorleyCell(
    var xi: Int,
    var yi: Int,
    var zi: Int,
) {

    constructor() : this(0, 0, 0)

    // offsets from the integer coordinates; in [0,1[
    var cellX = 0f
    var cellY = 0f
    var cellZ = 0f

    var distance = 0f

    fun set(other: WorleyCell?) {
        other ?: return

        xi = other.xi
        yi = other.yi
        zi = other.zi

        cellX = other.cellX
        cellY = other.cellY
        cellZ = other.cellZ

        distance = other.distance
    }

    fun set(oxi: Int, ocx: Float, odis: Float) {
        xi = oxi
        cellX = ocx
        distance = odis
    }

    fun set(oxi: Int, ocx: Float, oyi: Int, ocy: Float, odis: Float) {
        xi = oxi
        yi = oyi

        cellX = ocx
        cellY = ocy

        distance = odis
    }

    fun set(oxi: Int, ocx: Float, oyi: Int, ocy: Float, ozi: Int, ocz: Float, odis: Float) {
        xi = oxi
        yi = oyi
        zi = ozi

        cellX = ocx
        cellY = ocy
        cellZ = ocz

        distance = odis
    }

    fun sqrt(): Float {
        distance = kotlin.math.sqrt(distance)
        return distance
    }
}