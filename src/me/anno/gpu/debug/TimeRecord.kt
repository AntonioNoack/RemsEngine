package me.anno.gpu.debug

data class TimeRecord(
    val name: String,
    var deltaNanos: Long,
    var divisor: Int,
    val depth: Int,
) {
    val children = ArrayList<TimeRecord>()
}
