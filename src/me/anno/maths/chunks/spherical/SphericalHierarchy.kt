package me.anno.maths.chunks.spherical

import me.anno.maths.chunks.PlayerLocation
import me.anno.ecs.components.mesh.Mesh
import org.joml.Vector3d

/**
 * 2D LOD system for spherical worlds, e.g., planets
 * on SphericalTriangles, use .data to save your custom assigned data (e.g. visuals, statistics, ...)
 * */
open class SphericalHierarchy(
    radius: Double,
    shape: Mesh,
    var maxLevels: Int = 15, // 15 lods ~ max 32000² per mesh triangle
) {

    val triangles = Array(shape.numPrimitives.toInt()) { SphereTriangle(0, it + 1) }

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
        shape.forEachTriangle { a, b, c ->
            triangles[i++].set(this, a, b, c, radius)
        }
    }

    // todo additionally to this 2d spherical map, we could use a 3d spherical map: height split with binary tree as well

    // todo there should be two spherical mesh types: one with height map, and one with volumetric terrain
    // todo a spherical mesh has a basic shape, from which other sectors are found by subdivision
    // todo the meshes itself ofc should have more than 1 face, so either merge them into one optimized structure of faces,
    // todo or only go down more fine, if we really need it dearly (e.g. 10% of screen or sth like that)

    fun getTopLevelTriangle(v: Vector3d): SphereTriangle {
        var bestMatch: SphereTriangle = triangles[0]
        var bestDistance = Double.POSITIVE_INFINITY
        for (triangle in triangles) {
            if (triangle.containsGlobalPoint(v)) return triangle
            val distance = triangle.globalCenter.distanceSquared(v)
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
        if (triangle.childXX == null) {
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
                removeDistancedTriangles(triangle.childXX!!, players, loadingDistance, unloadingDistance)
                removeDistancedTriangles(triangle.childAB!!, players, loadingDistance, unloadingDistance)
                removeDistancedTriangles(triangle.childBC!!, players, loadingDistance, unloadingDistance)
                removeDistancedTriangles(triangle.childCA!!, players, loadingDistance, unloadingDistance)
            }
        }
    }

    fun removeChildren(triangle: SphereTriangle) {
        onDestroyChildren(triangle)
        triangle.childXX = null
        triangle.childAB = null
        triangle.childBC = null
        triangle.childCA = null
    }

    /**
     * iterate over all loaded triangles of all LODs, e.g., to save them
     * */
    fun forEach(shallCheckChildren: (SphereTriangle) -> Boolean) =
        forEach(shallCheckChildren, maxLevels)

    /**
     * iterate over all loaded triangles of all LODs, e.g., to save them
     * */
    fun forEach(shallCheckChildren: (SphereTriangle) -> Boolean, maxLevels: Int) {
        for (triangle in triangles) {
            triangle.forEach(maxLevels, shallCheckChildren)
        }
    }


}