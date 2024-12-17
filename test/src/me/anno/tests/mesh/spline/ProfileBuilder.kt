package me.anno.tests.mesh.spline

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.utils.Color.white
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs

class ProfileBuilder(mesh: Mesh) {

    private val pos: FloatArray = mesh.positions!!
    private val uvs: FloatArray? = mesh.uvs
    private val colors: IntArray? = mesh.color0

    private val connectivity = HashMap<Vector3f, ArrayList<Vector3f>>()
    private val uvMap = HashMap<Vector3f, Float>()
    private val colorMap = HashMap<Vector3f, Int>()

    fun isOnLine(p: FloatArray, i: Int): Boolean {
        return abs(p[i * 3 + 2]) < 0.03f
    }

    /**
     * returns point, if it is on the line
     * */
    private fun addPoint(index: Int): Vector3f? {
        println(Vector3f(pos, index * 3))
        return if (isOnLine(pos, index)) {
            val point = Vector3f(pos, index * 3)
            if (uvs != null && index * 2 + 1 < uvs.size) {
                uvMap[point] = uvs[index * 2]
            }
            if (colors != null && index < colors.size) {
                colorMap[point] = colors[index]
            }
            point
        } else null
    }

    private fun addLink(a: Vector3f, b: Vector3f) {
        connectivity.getOrPut(a, ::ArrayList).add(b)
    }

    fun addLine(ai: Int, bi: Int) {
        val a = addPoint(ai)
        val b = addPoint(bi)
        if (a != null && b != null) {
            addLink(a, b)
            addLink(b, a)
        }
    }

    fun build(): SplineProfile? {
        val starts = connectivity.filter { it.value.size == 1 }
        val start = starts.keys.minByOrNull { it.x } ?: return null
        val pointList = ArrayList<Vector3f>()
        pointList.add(start)
        while (true) {
            val point = pointList.last()
            val predecessor = pointList.getOrNull(pointList.size - 2)
            val nextPoints = connectivity[point]!!
            val nextPoint = nextPoints.firstOrNull2 { it != predecessor } ?: break
            pointList.add(nextPoint)
        }
        val dst = SplineProfile()
        dst.positions = pointList.map { Vector2f(it.x, it.y) }
        if (uvMap.isNotEmpty()) {
            val uvs1 = FloatArrayList(pointList.size)
            for (i in pointList.indices) {
                uvs1.add(uvMap[pointList[i]] ?: 0f)
            }
            dst.uvs = uvs1
        }
        if (colorMap.isNotEmpty()) {
            val col1 = IntArrayList(pointList.size)
            for (i in pointList.indices) {
                col1.add(colorMap[pointList[i]] ?: white)
            }
            dst.colors = col1
        }
        return dst
    }
}
