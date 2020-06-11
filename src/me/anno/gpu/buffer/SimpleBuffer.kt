package me.anno.gpu.buffer

import org.joml.Vector2f

class SimpleBuffer(val vertices: Array<Vector2f>, name: String): StaticFloatBuffer(listOf(
    Attribute(name, 2)
), vertices.size){

    init {
        vertices.forEach {
            put(it.x)
            put(it.y)
        }
    }

    constructor(vertices: Array<Vector2f>, indices: IntArray, name: String): this(
        createArray(
            vertices,
            indices
        ), name)

    companion object {

        fun createArray(vertices: Array<Vector2f>, indices: IntArray): Array<Vector2f> {
            return Array(indices.size){
                vertices[indices[it]]
            }
        }

        val flat01 = SimpleBuffer(
            arrayOf(
                Vector2f(0f, 0f),
                Vector2f(0f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, 0f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "attr0"
        )

        val flat11 = SimpleBuffer(
            arrayOf(
                Vector2f(-1f, -1f),
                Vector2f(-1f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, -1f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "attr0"
        )

    }

}