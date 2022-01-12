package me.anno.ecs.components.chunks.spherical

import me.anno.ecs.components.chunks.PlayerLocation
import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * 2D LOD system for spherical worlds, e.g. planets
 * on SphericalTriangles, use .data to save your custom assigned data (e.g. visuals, statistics, ...)
 * */
open class SphericalHierarchy(
    radius: Double,
    shape: Mesh,
    var maxLevels: Int = 15, // 15 lods ~ max 32000² per mesh triangle
) {

    val triangles = Array(shape.numTriangles) { SphereTriangle(it + 1) }

    open var radius = radius
        set(value) {
            if (field != value && value > 0.0) {
                field = value
                // when the radius is changed, all triangle positions & transforms need to be recomputed
                // this can be very expensive
                for (t in triangles) {
                    t.set(this, t.globalA, t.globalB, t.globalC, radius)
                }
            }
        }

    init {
        var i = 0
        shape.forEachTriangle { a: Vector3f, b: Vector3f, c: Vector3f ->
            triangles[i++].set(this, a, b, c, radius)
        }
    }

    // todo additionally to this 2d spherical map, we could use a 3d spherical map: height split with binary tree as well

    // todo there should be two spherical mesh types: one with height map, and one with volumetric terrain
    // todo a spherical mesh has a basic shape, from which other sectors are found by subdivision
    // todo the meshes itself ofc should have more than 1 face, so either merge them into one optimized structure of faces,
    // todo or only go down more fine, if we really need it dearly (e.g. 10% of screen or sth like that)

    // one object per triangle probably is a bad idea...
    // todo -> when a triangle is split, use it's 4 children

    // todo draw onto that sphere maybe
    // var subdivisionsPerTriangle = 32


    fun getTopLevelTriangle(v: Vector3d): SphereTriangle {
        var bestMatch: SphereTriangle = triangles[0]
        var bestDistance = Double.POSITIVE_INFINITY
        for (triangle in triangles) {
            if (triangle.containsGlobalPoint(v)) {
                return triangle
            }
            val distance = triangle.center.distanceSquared(v)
            if (distance < bestDistance) {
                bestDistance = distance
                bestMatch = triangle
            }
        }
        return bestMatch
    }

    /**
     * load all custom data
     * */
    open fun onCreateChildren(triangle: SphereTriangle) {}

    /**
     * save all custom data, destroy all objects
     * */
    open fun onDestroyChildren(triangle: SphereTriangle) {}

    fun getTriangle(v: Vector3d, maxTriangleSize: Double, generateIfMissing: Boolean): SphereTriangle {
        var bestTriangle = getTopLevelTriangle(v)
        for (depth in 0 until maxLevels) {
            val child = bestTriangle.getChildAtGlobal(v, generateIfMissing, ::onCreateChildren)
                ?: break
            bestTriangle = child
            if (child.size <= maxTriangleSize) break
        }
        return bestTriangle
    }

    fun removeDistancedTriangles(
        players: List<PlayerLocation>,
        loadingDistance: Double,
        unloadingDistance: Double
    ) {
        for (t in triangles) {
            removeDistancedTriangles(
                t, players,
                loadingDistance,
                unloadingDistance
            )
        }
    }

    fun removeDistancedTriangles(
        triangle: SphereTriangle,
        players: List<PlayerLocation>,
        loadingDistance: Double,
        unloadingDistance: Double,
    ) {
        // if children == null && close enough, generate children
        if (triangle.children == null) {
            if (players.any {
                    val dist = triangle.distanceToGlobal(it.x, it.y, it.z)
                    dist * it.loadMultiplier < loadingDistance
                }) {
                triangle.generateChildren()
            }
        } else {
            if (triangle.parent is SphereTriangle && players.all {
                    val dist = triangle.distanceToGlobal(it.x, it.y, it.z)
                    dist * it.unloadMultiplier > unloadingDistance
                }) {
                removeChildren(triangle)
            } else {
                for (child in triangle.children ?: return) {
                    removeDistancedTriangles(child, players, loadingDistance, unloadingDistance)
                }
            }
        }
    }

    fun removeChildren(triangle: SphereTriangle) {
        onDestroyChildren(triangle)
        triangle.children = null
    }

    /**
     * iterate over all loaded triangles of all LODs, e.g. to save them
     * */
    fun all(triangles: Array<SphereTriangle> = this.triangles): Sequence<SphereTriangle> {
        return sequence {
            for (triangle in triangles) {
                yield(triangle)
                val children = triangle.children
                if (children != null) {
                    yieldAll(all(children))
                }
            }
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(SphericalHierarchy::class)

        @JvmStatic
        fun main(args: Array<String>) {
            val a = Vector3d(-1.0, -1.0, 1.0)
            val b = Vector3d(-1.0, +1.0, 1.0)
            val c = Vector3d(+2.0, +0.0, 1.0)
            val tri = SphereTriangle(null, 0, a, b, c)
            LOGGER.info(tri.baseAB.print())
            LOGGER.info(tri.baseUp.print())
            LOGGER.info(tri.baseAC.print())
            LOGGER.info(tri.globalToLocal)
            LOGGER.info(tri.localToGlobal)
            LOGGER.info(tri.globalToLocal.transformPosition(Vector3d(0.0, 0.0, 1.1)).print()) // shall become (0,0.1,0)
            LOGGER.info(tri.globalToLocal.transformPosition(Vector3d(0.0, 1.0, 1.1)).print()) // shall become (1,0.1,0)
            LOGGER.info(tri.localA.print())
            LOGGER.info(tri.localB.print())
            LOGGER.info(tri.localC.print())
        }

    }

}