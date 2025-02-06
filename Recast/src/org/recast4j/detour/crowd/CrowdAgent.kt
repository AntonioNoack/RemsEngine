/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.crowd

import org.joml.Vector3f
import org.recast4j.Vectors
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.StraightPathItem
import org.recast4j.detour.crowd.Crowd.CrowdNeighbour
import kotlin.math.hypot
import kotlin.math.min

/**
 * Represents an agent managed by a #dtCrowd object.
 */
class CrowdAgent(val idx: Int) {
    /**
     * The type of navigation mesh polygon the agent is currently traversing.
     */
    enum class CrowdAgentState {
        /** The agent is not in a valid state.  */
        INVALID,

        /** The agent is traversing a normal navigation mesh polygon.  */
        WALKING,

        /** The agent is traversing an off-mesh connection.  */
        OFFMESH
    }

    enum class MoveRequestState {
        NONE, FAILED, VALID, REQUESTING, WAITING_FOR_QUEUE, WAITING_FOR_PATH, VELOCITY
    }

    /** The type of mesh polygon the agent is traversing. (See: #CrowdAgentState)  */
    var state: CrowdAgentState? = null

    /**
     * True if the agent has valid path (targetState == CROWDAGENT_TARGET_VALID), and the path does not lead to the
     * requested position, else false.
     */
    var partial = false

    /** The path corridor the agent is using.  */
    val corridor = PathCorridor()

    /** The local boundary data for the agent.  */
    val boundary = LocalBoundary()

    /** Time since the agent's path corridor was optimized.  */
    var topologyOptTime = 0f

    /** The known neighbors of the agent.  */
    val neis = ArrayList<CrowdNeighbour>()

    /** The desired speed.  */
    var desiredSpeed = 0f
    val currentPosition = Vector3f()

    /** A temporary value used to accumulate agent displacement during iterative collision resolution  */
    val disp = Vector3f()

    /** The desired velocity of the agent. Based on the current path, calculated from scratch each frame.  */
    val desiredVelocity = Vector3f()

    /** The desired velocity adjusted by obstacle avoidance, calculated from scratch each frame  */
    val desiredVelAdjusted = Vector3f()

    /** The actual velocity of the agent. The change from nvel -> vel is constrained by max acceleration  */
    val actualVelocity = Vector3f()

    /** The agent's configuration parameters.  */
    lateinit var params: CrowdAgentParams

    /** The local path corridor corners for the agent.  */
    val corners = ArrayList<StraightPathItem>()

    /** State of the movement request.  */
    var targetState: MoveRequestState? = null

    /** Target polyref of the movement request.  */
    var targetRef = 0L

    /** Target position of the movement request (or velocity in case of CROWDAGENT_TARGET_VELOCITY).  */
    val targetPosOrVel = Vector3f()

    /** Pathfinder query  */
    var targetPathQueryResult: PathQueryResult? = null

    /** Flag indicating that the current path is being replanned.  */
    var targetReplan = false

    /** Time since the agent's target was replanned.  */
    var targetReplanTime = 0f
    var targetReplanWaitTime = 0f
    val animation = CrowdAgentAnimation()

    fun integrate(dt: Float) {
        // Fake dynamic constraint.
        val maxDelta = params.maxAcceleration * dt
        val ds = desiredVelAdjusted.distance(actualVelocity)
        actualVelocity.lerp(desiredVelAdjusted, min(maxDelta / ds, 1f))

        // Integrate
        if (actualVelocity.lengthSquared() > 1e-8f) {
            actualVelocity.mulAdd(dt, currentPosition, currentPosition)
        } else actualVelocity.set(0f)
    }

    fun overOffmeshConnection(radius: Float): Boolean {
        if (corners.isEmpty()) return false
        val offMeshConnection = corners[corners.size - 1].flags and NavMeshQuery.DT_STRAIGHTPATH_OFFMESH_CONNECTION != 0
        if (offMeshConnection) {
            val distSq = Vectors.dist2DSqr(currentPosition, corners[corners.size - 1].pos)
            return distSq < radius * radius
        }
        return false
    }

    fun getDistanceToGoal(range: Float): Float {
        if (corners.isEmpty()) return range
        val endOfPath = corners[corners.size - 1].flags and NavMeshQuery.DT_STRAIGHTPATH_END != 0
        return if (endOfPath) min(Vectors.dist2D(currentPosition, corners[corners.size - 1].pos), range) else range
    }

    fun calcSmoothSteerDirection(dst: Vector3f): Vector3f {
        if (corners.isNotEmpty()) {
            val ip0 = 0
            val ip1 = min(1, corners.size - 1)
            val p0 = corners[ip0].pos
            val p1 = corners[ip1].pos
            val dir0x = p0.x - currentPosition.x
            val dir0z = p0.z - currentPosition.z
            var dir1x = p1.x - currentPosition.x
            var dir1z = p1.z - currentPosition.z
            val len0 = hypot(dir0x, dir0z)
            val len1 = hypot(dir1x, dir1z)
            if (len1 > 0.001f) {
                val sca = 1f / len1
                dir1x *= sca
                dir1z *= sca
            }
            dst.set(dir0x - dir1x * len0 * 0.5f, 0f, dir0z - dir1z * len0 * 0.5f).normalize()
        }
        return dst
    }

    fun calcStraightSteerDirection(dst: Vector3f): Vector3f {
        if (corners.isNotEmpty()) {
            dst.set(corners[0].pos).sub(currentPosition)
            dst.y = 0f
            dst.normalize()
        }
        return dst
    }

    fun setTarget(ref: Long, pos: Vector3f) {
        targetRef = ref
        targetPosOrVel.set(pos)
        targetPathQueryResult = null
        targetState = if (targetRef != 0L) MoveRequestState.REQUESTING else MoveRequestState.FAILED
    }
}