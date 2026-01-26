package org.recast4j.detour

import org.jetbrains.annotations.NotNull
import org.recast4j.detour.NavMeshDataCreateParams.Companion.b0
import org.recast4j.detour.NavMeshDataCreateParams.Companion.f0
import org.recast4j.detour.NavMeshDataCreateParams.Companion.i0

/**
 * @name Off-Mesh Connections Attributes (Optional)
 * Used to define a custom point-to-point edge within the navigation graph, an
 * off-mesh connection is a user defined traversable connection made up to two vertices,
 * at least one of which resides within a navigation mesh polygon.
 * */
class OffMeshConnections {

    /**
     * Off-mesh connection vertices. [(ax, ay, az, bx, by, bz) * #offMeshConCount]
     */
    @NotNull
    var fromTo: FloatArray = f0
        private set

    /**
     * Off-mesh connection radii. [Size: #offMeshConCount]
     */
    @NotNull
    var radius: FloatArray = f0
        private set

    /**
     * User defined flags assigned to the off-mesh connections. [Size: #offMeshConCount]
     */
    @NotNull
    var flags: IntArray = i0
        private set

    /**
     * User defined area ids assigned to the off-mesh connections. [Size: #offMeshConCount]
     */
    @NotNull
    var areaIds: IntArray = i0
        private set

    /**
     * The permitted travel direction of the off-mesh connections. [Size: #offMeshConCount]
     * 0 = Travel only from endpoint A to endpoint B. Bidirectional travel.
     * */
    @NotNull
    var isBidirectional: BooleanArray = b0
        private set

    /** The user defined ids of the off-mesh connection. [Size: #offMeshConCount] */
    @NotNull
    var userIds: IntArray = i0
        private set

    val capacity get() = radius.size

    /** The number of off-mesh connections. [Limit: >= 0] */
    var size = 0

    fun resize(newCapacity: Int) {
        fromTo = fromTo.copyOf(newCapacity * 6)
        radius = radius.copyOf(newCapacity)
        flags = flags.copyOf(newCapacity)
        areaIds = areaIds.copyOf(newCapacity)
        isBidirectional = isBidirectional.copyOf(newCapacity)
        userIds = userIds.copyOf(newCapacity)
    }
}