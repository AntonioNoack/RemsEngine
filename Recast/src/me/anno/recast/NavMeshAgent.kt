package me.anno.recast

import me.anno.ecs.Component
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.Vector3f
import org.recast4j.detour.DefaultQueryFilter
import org.recast4j.detour.FindRandomPointResult
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdAgent
import org.recast4j.detour.crowd.CrowdAgentParams
import java.util.Random

open class NavMeshAgent(
    val meshData: MeshData,
    val navMesh: org.recast4j.detour.NavMesh,
    val query: NavMeshQuery,
    val filter: DefaultQueryFilter,
    val random: Random,
    val navMesh1: NavMesh,
    val crowd: Crowd,
    val mask: Int,
    val maxSpeed: Float,
    val maxAcceleration: Float,
) : Component() {

    @NotSerializedProperty
    val params = CrowdAgentParams()

    @NotSerializedProperty
    var crowdAgent: CrowdAgent? = null

    fun init(): Boolean {
        val position = entity?.position ?: return false
        params.radius = navMesh1.agentRadius
        params.height = navMesh1.agentHeight
        params.maxSpeed = maxSpeed
        params.maxAcceleration = maxAcceleration
        params.collisionQueryRange = navMesh1.agentRadius * 2f
        params.pathOptimizationRange = params.collisionQueryRange * 1.5f
        // other params?
        crowdAgent = crowd.addAgent(Vector3f(position), params)
        return true
    }

    fun teleportTo(position: Vector3f) {
        val crowdAgent = crowdAgent ?: return
        crowd.resetMoveTarget(crowdAgent)
        crowdAgent.currentPosition.set(position)
        // todo where else is the position stored???
        // todo update the reference...
        // todo update the corridor
    }

    fun moveTo(position: Vector3f) {
        // todo half-extends need to be reasonable until we have a better algorithm implemented there in Recast4j, which doesn't iterate all valid tiles
        val crowdAgent = crowdAgent ?: return
        val nextPoly = query.findNearestPoly(position, Vector3f(100f), filter)
        if (nextPoly.succeeded()) {
            val result = nextPoly.result!!
            crowdAgent.setTarget(result.nearestRef, position)
        }
    }

    var maxRadius = 200f
    open fun findNextTarget() {
        val crowdAgent = crowdAgent ?: return
        val nextRef = query.findRandomPointWithinCircle(
            crowdAgent.corridor.lastPoly, crowdAgent.currentPosition,
            maxRadius, filter, random
        )
        if (nextRef != null) crowdAgent.setTarget(nextRef.randomRef, nextRef.randomPt)
        else lastWarning = "Cannot find random point within circle!"
    }

    override fun clone() = this
    override val className = "AgentController"
}