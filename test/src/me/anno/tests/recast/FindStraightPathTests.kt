package me.anno.tests.recast

import me.anno.image.ImageWriter
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.recast4j.LongArrayList
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMesh
import org.recast4j.detour.NavMesh.Companion.decodePolyIdPoly
import org.recast4j.detour.NavMesh.Companion.encodePolyId
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.Poly
import org.recast4j.detour.PortalResult
import kotlin.math.sin
import kotlin.random.Random

class FindStraightPathTests {

    private val pos = Vector2f()
    private var dir = 0f

    private val points = ArrayList<Vector2f>()

    private fun add(rx: Float, rz: Float) {
        val dx = Vector2f(1f, 0f).rotate(dir)
        val dz = Vector2f(0f, 1f).rotate(dir)

        points.add(pos + dx * rx + dz * rz)
    }

    private fun show(extra: List<Vector2f>) {
        val rand = Random(13254)
        val lines = ArrayList<ImageWriter.ColoredLine>()
        for (i in 2 until points.size) {
            val color = rand.nextInt().and(0x777777) or 0xff555555.toInt()
            val a = points[i - 2]
            val b = points[i - 1]
            val c = points[i]
            lines.add(ImageWriter.ColoredLine(a, b, color))
            lines.add(ImageWriter.ColoredLine(b, c, color))
            lines.add(ImageWriter.ColoredLine(c, a, color))
        }
        val color = -1
        for (i in 1 until extra.size) {
            lines.add(ImageWriter.ColoredLine(extra[i - 1], extra[i], color))
        }
        ImageWriter.writeLines(512, "straightPath.png", lines)
    }

    private fun init(): NavMeshQuery {
        val data = MeshData()
        data.vertCount = points.size
        data.vertices = FloatArray(data.vertCount * 3) {
            when (it % 3) {
                0 -> points[it / 3].x
                2 -> points[it / 3].y
                else -> 0f
            }
        }
        for (i in 0 until data.vertCount) {
            data.bounds.union(data.vertices, i * 3)
        }
        data.polyCount = points.size - 2
        data.polygons = Array(data.polyCount) {
            Poly(it, 3).apply {
                vertices[0] = it
                if (it % 2 == 0) {
                    vertices[1] = it + 2
                    vertices[2] = it + 1
                } else {
                    vertices[1] = it + 1
                    vertices[2] = it + 2
                }
                vertCount = 3
                if (it > 0) neighborData[0] = (it - 1) + 1
                if (it + 1 < data.polyCount) neighborData[1] = (it + 1) + 1
            }
        }
        val mesh = NavMesh(data, 3, 0)
        return NavMeshQuery(mesh)
    }

    @Test
    fun testStraightPath() {

        add(-1f, 0f)

        fun step(n: Int, ddir: Float) {
            for (i in 0 until n) {
                val dy = 0.2f * (1f + sin(i * 3.1416f / (n - 1f)))
                add(i.toFloat(), -dy)
                add(i.toFloat(), +dy)
            }

            pos.add(Vector2f(n.toFloat(), 0f).rotate(dir))
            dir += (ddir).toRadians()
        }

        step(5, 45f)
        step(5, -45f)
        step(5, -45f)

        val query = init()
        val path = LongArrayList()
        val tileId = 1
        val salt = query.nav1.getTile(tileId)!!.salt
        for (polyId in 0 until points.size - 2) {
            path.add(encodePolyId(salt, tileId, polyId))
        }

        println("path: ${(0 until path.size).map { polyId(path[it]) }}")

        val pl = points[points.size - 2] + points.last()
        val start = Vector3f(-0.5f, 0f, 0f)
        val end = Vector3f(pl.x, 0f, pl.y).mul(0.5f)

        val spi = query.findStraightPath(
            start, end, path, 20,
            0, PortalResult(),
            FloatArray(9),
            FloatArray(3), FloatArray(3),
            ArrayList()
        )!!

        println(spi.map { "[${it.pos}, ${polyId(it.ref)}, ${it.flags}]" })
        if (false) show(spi.map { Vector2f(it.pos.x, it.pos.z) })

        assertEquals(4, spi.size)
        assertEquals(start, spi[0].pos)
        assertEquals(Vector3f(4.8585787f, 0f, 0.14142136f), spi[1].pos)
        assertEquals(Vector3f(8.535534f, 0f, 3.3355339f), spi[2].pos)
        assertEquals(end, spi[3].pos)

        assertEquals(0, decodePolyIdPoly(spi[0].ref))
        assertEquals(12, decodePolyIdPoly(spi[1].ref))
        assertEquals(21, decodePolyIdPoly(spi[2].ref))
        assertEquals(0, decodePolyIdPoly(spi[3].ref))
    }

    fun polyId(ref: Long): String {
        return "[p${decodePolyIdPoly(ref)}]"
    }
}