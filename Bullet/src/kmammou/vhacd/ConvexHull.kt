package kmammou.vhacd

import com.bulletphysics.util.IntArrayList
import org.joml.AABBd
import org.joml.Vector3d

class ConvexHull(val vertices: List<Vector3d>, val triangles: IntArrayList) {

    var meshId = -1

    val bounds by lazy {
        val bounds = AABBd()
        for (i in vertices.indices) {
            bounds.union(vertices[i])
        }
        bounds
    }

    val centroid = lazy {
        TODO()
    }

    val volume: Double by lazy {
        computeConvexHullVolume(this)
    }
}