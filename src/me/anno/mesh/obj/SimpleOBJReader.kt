package me.anno.mesh.obj

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

/**
 * (on-purpose) very limited .obj-file reader: shall be quick to load
 * */
class SimpleOBJReader(input: InputStream) : TextFileReader(input) {

    private val positions = FloatArrayList(256 * 3)
    private val indices = IntArrayList(256)
    private val points = IntArrayList(5)

    private fun readCoordinate() {
        skipSpaces()
        positions.add(readFloat())
    }

    private fun readPosition() {
        readCoordinate()
        readCoordinate()
        readCoordinate()
    }

    private fun readFace() {
        val points = points
        while (points.size < 5) { // more can't be handled anyway
            skipSpaces()
            val next = next()
            if (next in 48..58) {
                putBack(next)
                points.add(readInt() - 1)
            } else break
        }
        val pattern = patterns.getOrNull(points.size - 1)
        if (pattern != null) {
            for (i in pattern) {
                indices.add(points[i])
            }
        } else {
            LOGGER.warn("Skipped face with {} points", points.size)
        }
        points.clear()
    }

    val mesh = Mesh()

    init {
        try {
            while (true) {
                // read the line
                skipSpaces()
                when (nextChar()) {
                    'v' -> readPosition()
                    'f' -> readFace()
                    else -> {}
                }
                skipLine()
            }
        } catch (_: EOFException) {
        }
        mesh.positions = positions.toFloatArray()
        mesh.indices = indices.toIntArray()
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(SimpleOBJReader::class)

        @JvmStatic
        private val patterns = arrayOf(
            intArrayOf(0, 0, 0),
            intArrayOf(0, 1, 1),
            intArrayOf(0, 1, 2),
            intArrayOf(0, 1, 2, 2, 3, 0)
        )
    }
}