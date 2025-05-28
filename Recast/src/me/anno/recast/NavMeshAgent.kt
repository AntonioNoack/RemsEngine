package me.anno.recast

import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.Vector3f
import org.recast4j.detour.FindRandomPointResult
import org.recast4j.detour.crowd.CrowdAgent
import org.recast4j.detour.crowd.CrowdAgentParams
import kotlin.random.Random

open class NavMeshAgent(val data: NavMeshData) : Component(), OnUpdate {

    @NotSerializedProperty
    val params = CrowdAgentParams()

    @NotSerializedProperty
    var crowdAgent: CrowdAgent? = null

    fun init(): Boolean {
        val position = entity?.position ?: return false
        val agentType = data.agentType
        params.radius = agentType.radius
        params.height = agentType.height
        params.maxSpeed = agentType.maxSpeed
        params.maxAcceleration = agentType.maxAcceleration
        params.collisionQueryRange = agentType.radius * 2f
        params.pathOptimizationRange = params.collisionQueryRange * 1.5f
        // other params?
        crowdAgent = data.crowd.addAgent(Vector3f(position), params)
        return true
    }

    override fun onDisable() {
        super.onDisable()
        val agent = crowdAgent ?: return
        data.crowd.removeAgent(agent)
        agent.actualVelocity.set(0f)
        crowdAgent = null
    }

    fun teleportTo(position: Vector3f) {
        val crowdAgent = crowdAgent ?: return
        data.crowd.resetMoveTarget(crowdAgent)
        crowdAgent.currentPosition.set(position)
        // todo where else is the position stored???
        // todo update the reference...
        // todo update the corridor
    }

    fun moveTo(position: Vector3f) {
        val crowdAgent = crowdAgent ?: return
        val nextPoly = data.query.findNearestPoly(position, data.filter)
        if (nextPoly.succeeded()) {
            val result = nextPoly.result!!
            crowdAgent.setTarget(result.nearestRef, position)
        }
    }

    override fun onUpdate() {
        if (crowdAgent == null) init()
    }

    open fun moveToRandomPoint(random: Random) {
        val crowdAgent = crowdAgent ?: return
        val nextRef = findRandomTarget(data, crowdAgent, Float.POSITIVE_INFINITY, random)
        if (nextRef != null) crowdAgent.setTarget(nextRef.randomRef, nextRef.randomPt)
        else lastWarning = "Cannot find random point within circle!"
    }

    override fun clone() = this
    override val className = "AgentController"

    companion object {
        fun findRandomTarget(
            data: NavMeshData, crowdAgent: CrowdAgent,
            maxRadius: Float, random: Random
        ): FindRandomPointResult? {
            return data.query.findRandomPointWithinCircle(
                crowdAgent.corridor.lastPoly, crowdAgent.currentPosition,
                maxRadius, data.filter, random
            )
        }
    }
}