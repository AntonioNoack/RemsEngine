package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachLine
import me.anno.engine.DefaultAssets.flatCube
import me.anno.mesh.FindLines.makeLineMesh
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.max
import kotlin.math.min

object MeshToLineTests {

    @Test
    fun testMakeLineMeshUnique() {
        val lineMesh = flatCube.shallowClone()
        lineMesh.makeLineMesh(keepOnlyUniqueLines = true)

        val baseLines = LinkedHashSet<Pair<Vector3f,Vector3f>>()
        flatCube.forEachLine { a, b ->
            val line = Vector3f(a) to Vector3f(b)
            baseLines.add(line)
            false
        }

        val baseLinesList =
            baseLines.toMutableList().apply { reverse() }

        lineMesh.forEachLine { a, b ->
            val line = Vector3f(a) to Vector3f(b)
            assertEquals(baseLinesList.removeLast(), line)
            false
        }
        assertTrue(baseLinesList.isEmpty())
    }

    @Test
    fun testMakeLineMeshWithDuplicates() {
        val lineMesh = flatCube.shallowClone()
        lineMesh.makeLineMesh(keepOnlyUniqueLines = false)

        val baseLines = ArrayList<Pair<Vector3f,Vector3f>>()
        flatCube.forEachLine { a, b ->
            val line = Vector3f(a) to Vector3f(b)
            baseLines.add(line)
            false
        }

        val baseLinesList =
            baseLines.apply { reverse() }

        lineMesh.forEachLine { a, b ->
            val line = Vector3f(a) to Vector3f(b)
            assertEquals(baseLinesList.removeLast(), line)
            false
        }
        assertTrue(baseLinesList.isEmpty())
    }

    @Test
    fun testForEachLine() {
        val base = flatCube
        val points = mapOf(
            Vector3f(-1f, -1f, -1f) to 0,
            Vector3f(-1f, -1f, +1f) to 1,
            Vector3f(-1f, +1f, -1f) to 2,
            Vector3f(-1f, +1f, +1f) to 3,
            Vector3f(+1f, -1f, -1f) to 4,
            Vector3f(+1f, -1f, +1f) to 5,
            Vector3f(+1f, +1f, -1f) to 6,
            Vector3f(+1f, +1f, +1f) to 7,
        )
        val lines = listOf(
            0 to 1, 0 to 2, 0 to 4,
            1 to 3, 1 to 5,
            2 to 3, 2 to 6,
            3 to 7,
            4 to 5, 4 to 6,
            5 to 7,
            6 to 7,
        )
        val counters = IntArray(lines.size)
        base.forEachLine { a, b ->
            assertNotEquals(a, b)
            val ai = points[a]!!
            val bi = points[b]!!
            val min = min(ai, bi)
            val max = max(ai, bi)
            val idx = lines.indexOf(min to max)
            if (idx >= 0) counters[idx]++
            false
        }
        assertTrue(counters.all { it == 2 })
    }
}