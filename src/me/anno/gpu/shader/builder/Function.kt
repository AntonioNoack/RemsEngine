package me.anno.gpu.shader.builder

data class Function(
    val name: String,
    val header: String,
    val body: String
) {
    constructor(body: String) : this("", "", body)
}
