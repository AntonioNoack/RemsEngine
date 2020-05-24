package me.anno.fonts.mesh

import me.anno.utils.isInsideTriangle
import org.joml.Vector2f

object Triangulator {

    fun ringToTriangles(pts: List<Vector2f>) =
        ringToTriangleIndices(pts).map { pts[it] }.toMutableList()

    // operator fun Vector2f.minus(s: Vector2f) = Vector2f(x-s.x, y-s.y)

    fun getGuessArea(pts: List<Vector2f>): Float {
        val n = pts.size
        var area = 0f
        var p = n-1
        var q = 0
        while(q < n){
            val pv = pts[p]
            val qv = pts[q]
            area += pv.x * qv.y - qv.x * pv.y // cross product
            p = q++
        }
        return area * 0.5f
    }

    fun ringToTriangleIndices(pts: List<Vector2f>): List<Int> {

        val triangles = ArrayList<Int>()
        val n = pts.size
        if(n < 3) return triangles

        val areaForSign = getGuessArea(pts)
        val indices = if(areaForSign > 0){
            IntArray(n){ it }
        } else {
            IntArray(n){ n-1-it }
        }

        fun snip(u: Int, v: Int, w: Int, n: Int): Boolean {

            val a = pts[indices[u]]
            val b = pts[indices[v]]
            val c = pts[indices[w]]

            val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
            if(area < 1e-7) return false

            for(p in 0 until n){
                if(p == u || p == v || p == w) continue
                val P = pts[indices[p]]
                if(P.isInsideTriangle(a, b, c)){
                    return false
                }
            }

            return true

        }

        var nv = n
        var count = 2 * nv

        var v = nv - 1
        while(nv > 2){

            if(count-- <= 0) return triangles

            var u = v
            if(nv <= u) u = 0
            v = u+1
            if(nv <= v) v = 0
            var w = v + 1
            if(nv <= w) w = 0

            if(snip(u, v, w, nv)){
                triangles += indices[u]
                triangles += indices[v]
                triangles += indices[w]
                var s = v
                var t = v + 1
                while(t < nv){

                    indices[s] = indices[t]

                    s++
                    t++
                }
                nv--
                count = 2 * nv
            }

        }

        return triangles

    }

}