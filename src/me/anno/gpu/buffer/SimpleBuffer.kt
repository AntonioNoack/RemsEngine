package me.anno.gpu.buffer

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths.pow
import org.joml.Vector2f

open class SimpleBuffer(name0: String, val vertices: Array<Vector2f>, name: String) :
    StaticBuffer(name0, listOf(Attribute(name, 2)), vertices.size) {

    constructor(name0: String, vertices: Array<Vector2f>, indices: IntArray, name: String) :
            this(name0, createArray(vertices, indices), name)

    init {
        val v = vertices
        for (i in v.indices) put(v[i])
    }

    companion object {

        @JvmStatic
        fun createArray(vertices: Array<Vector2f>, indices: IntArray): Array<Vector2f> {
            return Array(indices.size) {
                vertices[indices[it]]
            }
        }

        // to do "move" towards the viewer for large distance, so it stays fullscreen?
        // like a sphere?
        // or add a sphere additionally? (then without our effects)
        @JvmStatic
        private fun createFlatLarge(): StaticBuffer {
            val step = 10f
            val iList = -10..10
            val buffer = StaticBuffer(
                "flatLarge",
                listOf(Attribute("coords", 2)),
                6 * (4 * iList.toList().size - 3)
            )
            for ((index, i) in iList.withIndex()) {
                val l = pow(step, i.toFloat())
                val s = l / step
                if (index == 0) {
                    // first face: just a quad
                    buffer.put(-l, -l)
                    buffer.put(-l, +l)
                    buffer.put(+l, +l)
                    buffer.put(-l, -l)
                    buffer.put(+l, +l)
                    buffer.put(+l, -l)
                } else {
                    // secondary faces: quad rings
                    for (j in 0 until 4) {
                        fun put(x0: Float, y0: Float) {
                            when (j) {
                                0 -> buffer.put(+x0, +y0)
                                1 -> buffer.put(-x0, -y0)
                                2 -> buffer.put(-y0, +x0)
                                3 -> buffer.put(+y0, -x0)
                            }
                        }

                        put(+s, +s) // 0
                        put(+l, +l) // 1
                        put(-l, +l) // 2

                        put(+s, +s) // 0
                        put(-l, +l) // 2
                        put(-s, +s) // 3
                    }
                }
            }
            return buffer
        }

        /**
         * The go-to buffer for applying a shader to a rectangle of the screen, or all of it.
         * If you want to draw a sub-rect only, please take a look at GFXx2D.posSize().
         * */
        @JvmField
        val flat01 = object : SimpleBuffer(
            "flat01", arrayOf(
                Vector2f(0f, 0f),
                Vector2f(0f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, 0f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "coords"
        ) {
            // https://wallisc.github.io/rendering/2021/04/18/Fullscreen-Pass.html
            private val flat01FS = SimpleBuffer(
                "flat01FS", arrayOf(
                    Vector2f(0f, 0f),
                    Vector2f(2f, 0f),
                    Vector2f(0f, 2f),
                ), intArrayOf(0, 1, 2), "coords"
            )

            override fun draw(shader: Shader) {
                // this draws full-screen passes like blur using a single triangle
                // if you dislike this behaviour, create your own buffer 😉
                if (Frame.isFullscreen() && shader.getUniformLocation("posSize", false) < 0)
                    flat01FS.draw(shader)
                else super.draw(shader)
            }
        }

        @JvmStatic
        fun splitIndices(numSegments: Int): IntArray {
            val size = (numSegments - 1) * 6
            val idx = IntArray(size)
            var i = 0
            for (xi in 0 until numSegments - 1) {
                val o = xi * 2
                idx[i++] = o
                idx[i++] = o + 1
                idx[i++] = o + 2
                idx[i++] = o + 1
                idx[i++] = o + 2
                idx[i++] = o + 3
            }
            return idx
        }

        @JvmStatic
        fun splitVertices(numSegments: Int): FloatArray {
            val size = numSegments * 4
            val idx = FloatArray(size)
            var i = 0
            for (xi in 0 until numSegments) {
                val t = xi / (numSegments - 1f)
                idx[i++] = t
                idx[i++] = -1f
                idx[i++] = t
                idx[i++] = +1f
            }
            return idx
        }

        @JvmField
        val flat11x2 = StaticBuffer(
            "flat11x2",
            splitVertices(2),
            splitIndices(2),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flat11x3 = StaticBuffer(
            "flat11x3",
            splitVertices(3),
            splitIndices(3),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flat11x6 = StaticBuffer(
            "flat11x6",
            splitVertices(6),
            splitIndices(6),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flat11x12 = StaticBuffer(
            "flat11x12",
            splitVertices(12),
            splitIndices(12),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flat11x25 = StaticBuffer(
            "flat11x25",
            splitVertices(25),
            splitIndices(25),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flat11x50 = StaticBuffer(
            "flat11x50",
            splitVertices(50),
            splitIndices(50),
            listOf(Attribute("coords", 2))
        )

        @JvmField
        val flatLarge = createFlatLarge()

        @JvmField
        val flat01Mesh = Mesh().apply {
            positions = floatArrayOf(
                -1f, -1f, 0f,
                -1f, +1f, 0f,
                +1f, +1f, 0f,
                +1f, -1f, 0f,
            )
            uvs = floatArrayOf(
                0f, 1f,
                0f, 0f,
                1f, 0f,
                1f, 1f,
            )
            indices = intArrayOf(0, 1, 2, 0, 2, 3)
        }

        @JvmStatic
        val flat01CubeX10 by lazy {

            // create a fine grid
            val sizeX = 20
            val sizeY = 20

            val quadCount = sizeX * sizeY
            val vertexCount = quadCount * 6
            val positions = FloatArray(vertexCount * 3)
            val uvs = FloatArray(vertexCount * 2)

            val di = 1f / sizeX
            val dj = 1f / sizeY

            var i0 = 0
            var i1 = 0
            fun put(x: Float, y: Float) {
                positions[i0++] = x * 2f - 1f
                positions[i0++] = y * 2f - 1f
                positions[i0++] = 0f
                uvs[i1++] = x
                uvs[i1++] = y
            }

            for (i in 0 until sizeX) {
                val i01 = i.toFloat() / sizeX
                for (j in 0 until sizeY) {
                    val j01 = j.toFloat() / sizeY

                    put(i01, j01)
                    put(i01 + di, j01)
                    put(i01 + di, j01 + dj)

                    put(i01, j01)
                    put(i01 + di, j01 + dj)
                    put(i01, j01 + dj)
                }
            }

            val mesh = Mesh()
            mesh.positions = positions
            mesh.uvs = uvs
            mesh
        }

        @JvmField
        val flat11 = SimpleBuffer(
            "flat11",
            arrayOf(
                Vector2f(-1f, -1f),
                Vector2f(-1f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, -1f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "coords"
        )

        @JvmStatic
        val circle by lazy {
            val n = 36 * 4
            // angle, scaling
            val buffer = StaticBuffer(
                "circle",
                listOf(Attribute("coords", 2)),
                3 * 2 * n
            )

            fun put(index: Int, scaling: Float) {
                buffer.put(index.toFloat() / n, scaling)
            }
            for (i in 0 until n) {
                val j = i + 1
                put(i, 0f)
                put(i, 1f)
                put(j, 1f)
                put(i, 0f)
                put(j, 1f)
                put(j, 0f)
            }
            buffer
        }
    }
}