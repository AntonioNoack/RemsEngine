package me.anno.gpu.buffer

import me.anno.utils.pow
import org.joml.Vector2f

class SimpleBuffer(val vertices: Array<Vector2f>, name: String) : StaticBuffer(
    listOf(
        Attribute(name, 2)
    ), vertices.size
) {

    init {
        vertices.forEach {
            put(it.x)
            put(it.y)
        }
    }

    constructor(vertices: Array<Vector2f>, indices: IntArray, name: String) : this(
        createArray(
            vertices,
            indices
        ), name
    )

    companion object {

        fun createArray(vertices: Array<Vector2f>, indices: IntArray): Array<Vector2f> {
            return Array(indices.size) {
                vertices[indices[it]]
            }
        }

        // todo "move" towards the viewer for large distance, so it stays fullscreen?
        // todo like a sphere?
        // todo or add a sphere additionally? (then without our effects)
        private fun createFlatLarge(): StaticBuffer {
            val step = 10f
            val iList = -10 .. 10
            val buffer = StaticBuffer(
                listOf(Attribute("attr0", 2)),
                4 * (4 * iList.toList().size - 3)
            )
            buffer.quads()
            for((index, i) in iList.withIndex()){
                val l = pow(step, i.toFloat())
                val s = l / step
                if(index == 0){
                    // first face: just a quad
                    buffer.put(-l, -l)
                    buffer.put(-l, l)
                    buffer.put(l, l)
                    buffer.put(l, -l)
                } else {
                    // secondary faces: quad rings
                    for(j in 0 until 4){
                        fun put(x0: Float, y0: Float){
                            when(j){
                                0 -> buffer.put(+x0, +y0)
                                1 -> buffer.put(-x0, -y0)
                                2 -> buffer.put(-y0, +x0)
                                3 -> buffer.put(+y0, -x0)
                            }
                        }
                        put(s, -s)
                        put(s, s)
                        put(l, l)
                        put(l, -l)
                    }
                }
            }
            return buffer
        }

        val flat01 = SimpleBuffer(
            arrayOf(
                Vector2f(0f, 0f),
                Vector2f(0f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, 0f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "attr0"
        )

        val flatLarge = createFlatLarge()

        val flat01Cube = StaticBuffer(
            listOf(
                listOf(-1f, -1f, 0f, 0f, 0f),
                listOf(-1f, +1f, 0f, 0f, 1f),
                listOf(+1f, +1f, 0f, 1f, 1f),
                listOf(+1f, -1f, 0f, 1f, 0f)
            ),
            listOf(
                Attribute("attr0", 3),
                Attribute("attr1", 2)
            ),
            intArrayOf(0, 1, 2, 0, 2, 3)
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