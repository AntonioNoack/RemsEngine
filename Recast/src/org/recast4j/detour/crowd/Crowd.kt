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
import org.recast4j.LongArrayList
import org.recast4j.Vectors
import org.recast4j.detour.*
import org.recast4j.detour.NavMeshQuery.Companion.MAX_NEIS
import org.recast4j.detour.crowd.CrowdAgent.CrowdAgentState
import org.recast4j.detour.crowd.CrowdAgent.MoveRequestState
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams
import org.recast4j.detour.crowd.debug.CrowdAgentDebugInfo
import org.recast4j.detour.crowd.debug.ObstacleAvoidanceDebugData
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Members in this module implement local steering and dynamic avoidance features.
 *
 *
 * The crowd is the big beast of the navigation features. It not only handles a lot of the path management for you, but
 * also local steering and dynamic avoidance between members of the crowd. I.e. It can keep your agents from running
 * into each other.
 *
 *
 * Main class: Crowd
 *
 *
 * The #dtNavMeshQuery and #dtPathCorridor classes provide perfectly good, easy to use path planning features. But in
 * the end they only give you points that your navigation client should be moving toward. When it comes to deciding
 * things like agent velocity and steering to avoid other agents, that is up to you to implement. Unless, of course, you
 * decide to use Crowd.
 *
 *
 * Basically, you add an agent to the crowd, providing various configuration settings such as maximum speed and
 * acceleration. You also provide a local target to move toward. The crowd manager then provides, with every update, the
 * new agent position and velocity for the frame. The movement will be constrained to the navigation mesh, and steering
 * will be applied to ensure agents managed by the crowd do not collide with each other.
 *
 *
 * This is very powerful feature set. But it comes with limitations.
 *
 *
 * The biggest limitation is that you must give control of the agent's position completely over to the crowd manager.
 * You can update things like maximum speed and acceleration. But in order for the crowd manager to do its thing, it
 * can't allow you to constantly be giving it overrides to position and velocity. So you give up direct control of the
 * agent's movement. It belongs to the crowd.
 *
 *
 * The second-biggest limitation revolves around the fact that the crowd manager deals with local planning. So the
 * agent's target should never be more than 256 polygons away from its current position. If it is, you risk your agent
 * failing to reach its target. So you may still need to do long distance planning and provide the crowd manager with
 * intermediate targets.
 *
 *
 * Other significant limitations:
 *
 *
 * - All agents using the crowd manager will use the same #dtQueryFilter. - Crowd management is relatively expensive.
 * The maximum agents under crowd management at any one time is between 20 and 30. A good place to start is a maximum of
 * 25 agents for 0.5ms per frame.
 *
 * @note This is a summary list of members. Use the index or search feature to find minor members.
 * struct dtCrowdAgentParams
 * var dtCrowdAgentParams::obstacleAvoidanceType
 * par #dtCrowd permits agents to use different avoidance configurations. This value is the index of the
 * #dtObstacleAvoidanceParams within the crowd.
 * var dtCrowdAgentParams::collisionQueryRange
 * par Collision elements include other agents and navigation mesh boundaries.
 *
 *
 * This value is often based on the agent radius and/or maximum speed. E.g. radius * 8
 * var dtCrowdAgentParams::pathOptimizationRange
 * par Only applicable if #updateFlags includes the #CROWD_OPTIMIZE_VIS flag.
 *
 *
 * This value is often based on the agent radius. E.g., radius * 30
 * var dtCrowdAgentParams::separationWeight
 * par A higher value will result in agents trying to stay farther away from each other at the cost of more difficult
 * steering in tight spaces.
 * see CrowdAgent, Crowd::addAgent(), Crowd::updateAgentParameters()
 * see dtObstacleAvoidanceParams, dtCrowd::setObstacleAvoidanceParams(), dtCrowd::getObstacleAvoidanceParams()
 * see dtPathCorridor::optimizePathVisibility()
 *
 *
 * This is the core class of the @ref crowd module. See the @ref crowd documentation for a summary of the crowd
 * features. A common method for setting up the crowd is as follows: -# Allocate the crowd -# Set the avoidance
 * configurations using #setObstacleAvoidanceParams(). -# Add agents using #addAgent() and make an initial movement
 * request using #requestMoveTarget(). A common process for managing the crowd is as follows: -# Call #update() to allow
 * the crowd to manage its agents. -# Retrieve agent information using #getActiveAgents(). -# Make movement requests
 * using #requestMoveTarget() when movement goal changes. -# Repeat every frame. Some agent configuration settings can
 * be updated using #updateAgentParameters(). But the crowd owns the agent position. So it is not possible to update an
 * active agent's position. If agent position must be fed back into the crowd, the agent must be removed and re-added.
 * Notes: - Path related information is available for newly added agents only after an #update() has been performed. -
 * Agent objects are kept in a pool and re-used. So it is important when using agent objects to check the value of
 * #dtCrowdAgent::active to determine if the agent is actually in use or not. - This class is meant to provide 'local'
 * movement. There is a limit of 256 polygons in the path corridor. So it is not meant to provide automatic pathfinding
 * services over long distances.
 *
 *
 * see dtAllocCrowd(), dtFreeCrowd(), init(), dtCrowdAgent
 */
class Crowd @JvmOverloads constructor(
    val config: CrowdConfig,
    nav: NavMesh,
    queryFilterFactory: (Int) -> QueryFilter = { DefaultQueryFilter() }
) {

    private var agentId = 0

    val activeAgents = HashSet<CrowdAgent>()

    private val obstacleAvoidanceQuery: ObstacleAvoidanceQuery =
        ObstacleAvoidanceQuery(config.maxObstacleAvoidanceCircles, config.maxObstacleAvoidanceSegments)
    val grid = ProximityGrid(config.maxAgentRadius * 3)
    val queryExtends = Vector3f(config.maxAgentRadius * 2f, config.maxAgentRadius * 1.5f, config.maxAgentRadius * 2f)
    private val filters = Array(CROWD_MAX_QUERY_FILTER_TYPE) { queryFilterFactory(it) }
    private val obstacleQueryParams = Array(CROWD_MAX_OBSTAVOIDANCE_PARAMS) { ObstacleAvoidanceParams() }
    val pathQueue: PathQueue = PathQueue(config)

    private var navQuery = NavMeshQuery(nav)
    private var navMesh: NavMesh = nav

    val telemetry = CrowdTelemetry()
    var velocitySampleCount = 0

    fun setNavMesh(nav: NavMesh) {
        navMesh = nav
        navQuery = NavMeshQuery(nav)
    }

    /**
     * Sets the shared avoidance configuration for the specified index.
     */
    fun setObstacleAvoidanceParams(idx: Int, params: ObstacleAvoidanceParams) {
        if (idx in 0 until CROWD_MAX_OBSTAVOIDANCE_PARAMS) {
            obstacleQueryParams[idx] = ObstacleAvoidanceParams(params)
        }
    }

    /**
     * Gets the shared avoidance configuration for the specified index.
     */
    fun getObstacleAvoidanceParams(idx: Int): ObstacleAvoidanceParams? {
        return if (idx in 0 until CROWD_MAX_OBSTAVOIDANCE_PARAMS) obstacleQueryParams[idx]
        else null
    }

    /**
     * Adds a new agent to the crowd.
     *
     * @param pos    The requested position of the agent. [(x, y, z)]
     * @param params The configutation of the agent.
     * @return The newly created agent object
     */
    fun addAgent(pos: Vector3f, params: CrowdAgentParams): CrowdAgent {
        val agent = CrowdAgent(agentId++)
        agent.params = params
        return addAgent(pos, agent)
    }

    /**
     * Adds a new agent to the crowd.
     *
     * @param pos    The requested position of the agent. [(x, y, z)]
     * @param params The configutation of the agent.
     * @return The newly created agent object
     */
    fun addAgent(pos: Vector3f, agent: CrowdAgent): CrowdAgent {
        activeAgents.add(agent)

        // Find nearest position on navmesh and place the agent there.
        val nearestPoly = navQuery.findNearestPoly(pos, queryExtends, filters[agent.params.queryFilterType])
        val nearest = if (nearestPoly.succeeded()) nearestPoly.result!!.nearestPos else pos
        val ref = if (nearestPoly.succeeded()) nearestPoly.result!!.nearestRef else 0L
        agent.corridor.reset(ref, nearest!!)
        agent.boundary.reset()
        agent.partial = false
        agent.topologyOptTime = 0f
        agent.targetReplanTime = 0f
        agent.desiredVelocity.set(0f)
        agent.desiredVelAdjusted.set(0f)
        agent.actualVelocity.set(0f)
        agent.currentPosition.set(nearest)
        agent.desiredSpeed = 0f
        if (ref != 0L) {
            agent.state = CrowdAgentState.WALKING
        } else {
            agent.state = CrowdAgentState.INVALID
        }
        agent.targetState = MoveRequestState.NONE
        return agent
    }

    /**
     * Removes the agent from the crowd.
     *
     * @param agent Agent to be removed
     */
    fun removeAgent(agent: CrowdAgent): Boolean {
        return activeAgents.remove(agent)
    }

    private fun requestMoveTargetReplan(ag: CrowdAgent, ref: Long, pos: Vector3f) {
        ag.setTarget(ref, pos)
        ag.targetReplan = true
    }

    /**
     * Submits a new move request for the specified agent.
     * @param ref The position's polygon reference.
     * @param pos The position within the polygon. [(x, y, z)]
     * @return whether the request was successfully submitted.
     *
     * This method is used when a new target is set.
     *
     * The position will be constrained to the surface of the navigation mesh.
     *
     * The request will be processed during the next #update().
     */
    fun requestMoveTarget(agent: CrowdAgent, ref: Long, pos: Vector3f): Boolean {
        if (ref == 0L) return false
        // Initialize request.
        agent.setTarget(ref, pos)
        agent.targetReplan = false
        return true
    }

    /**
     * Submits a new move request for the specified agent.
     * @param [in ] vel The movement velocity. [(x, y, z)]
     * @return whether the request was successfully submitted.
     */
    fun requestMoveVelocity(agent: CrowdAgent, vel: Vector3f): Boolean {
        // Initialize request.
        agent.targetRef = 0
        agent.targetPosOrVel.set(vel)
        agent.targetPathQueryResult = null
        agent.targetReplan = false
        agent.targetState = MoveRequestState.VELOCITY
        return true
    }

    /**
     * Resets any request for the specified agent.
     * @return True if the request was successfully reset.
     */
    fun resetMoveTarget(agent: CrowdAgent): Boolean {
        // Initialize request.
        agent.targetRef = 0
        agent.targetPosOrVel.set(0f)
        agent.desiredVelocity.set(0f)
        agent.targetPathQueryResult = null
        agent.targetReplan = false
        agent.targetState = MoveRequestState.NONE
        return true
    }

    fun getFilter(i: Int): QueryFilter? {
        return if (i in 0 until CROWD_MAX_QUERY_FILTER_TYPE) filters[i] else null
    }

    fun update(dt: Float, debug: CrowdAgentDebugInfo?): CrowdTelemetry {
        velocitySampleCount = 0
        telemetry.start()

        val agents = activeAgents
        // Check that all agents still have valid paths.
        checkPathValidity(agents, dt)

        // Update async move request and pathfinder.
        updateMoveRequest(agents, dt)

        // Optimize path topology.
        updateTopologyOptimization(agents, dt)

        // Register agents to proximity grid.
        buildProximityGrid(agents)

        // Get nearby navmesh segments and agents to collide with.
        buildNeighbours(agents)

        // Find next corner to steer to.
        findCorners(agents, debug)

        // Trigger off-mesh connections (depends on corners).
        triggerOffMeshConnections(agents)

        // Calculate steering.
        calculateSteering(agents)

        // Velocity planning.
        planVelocity(debug, agents)

        // Integrate.
        integrate(dt, agents)

        // Handle collisions.
        handleCollisions(agents)
        moveAgents(agents)

        // Update agents using off-mesh connection.
        updateOffMeshConnections(agents, dt)
        return telemetry
    }

    private fun checkPathValidity(agents: Collection<CrowdAgent>, dt: Float) {
        telemetry.start(CrowdTelemetryType.CHECK_PATH_VALIDITY)
        val agentPos = Vector3f()
        for (ag in agents) {
            checkPathValidity(ag, dt, agentPos)
        }
        telemetry.stop(CrowdTelemetryType.CHECK_PATH_VALIDITY)
    }

    private fun checkPathValidity(ag: CrowdAgent, dt: Float, agentPos: Vector3f) {
        if (ag.state != CrowdAgentState.WALKING) return

        ag.targetReplanTime += dt
        var replan = false

        // First check, that the current location is valid.
        var agentRef = ag.corridor.firstPoly
        agentPos.set(ag.currentPosition)
        if (!navQuery.isValidPolyRef(agentRef, filters[ag.params.queryFilterType])) {
            // Current location is not valid, try to reposition.
            // TODO: this can snap agents, how to handle that?
            val nearestPoly = navQuery.findNearestPoly(
                ag.currentPosition, queryExtends,
                filters[ag.params.queryFilterType]
            )
            agentRef = if (nearestPoly.succeeded()) nearestPoly.result!!.nearestRef else 0L
            if (nearestPoly.succeeded()) {
                agentPos.set(nearestPoly.result!!.nearestPos!!)
            }
            if (agentRef == 0L) {
                // Could not find location in navmesh, set state to invalid.
                ag.corridor.reset(0, agentPos)
                ag.partial = false
                ag.boundary.reset()
                ag.state = CrowdAgentState.INVALID
                return
            }

            // Make sure the first polygon is valid, but leave other valid
            // polygons in the path so that replanner can adjust the path
            // better.
            ag.corridor.fixPathStart(agentRef, agentPos)
            // ag.corridor.trimInvalidPath(agentRef, agentPos, m_navquery,
            // &m_filter);
            ag.boundary.reset()
            ag.currentPosition.set(agentPos)
            replan = true
        }

        // If the agent does not have move target or is controlled by
        // velocity, no need to recover the target nor replan.
        if (ag.targetState == MoveRequestState.NONE || ag.targetState == MoveRequestState.VELOCITY) {
            return
        }

        // Try to recover move request position.
        if (ag.targetState != MoveRequestState.FAILED) {
            if (!navQuery.isValidPolyRef(ag.targetRef, filters[ag.params.queryFilterType])) {
                // Current target is not valid, try to reposition.
                val fnp = navQuery.findNearestPoly(
                    ag.targetPosOrVel, queryExtends,
                    filters[ag.params.queryFilterType]
                )
                ag.targetRef = if (fnp.succeeded()) fnp.result!!.nearestRef else 0L
                if (fnp.succeeded()) {
                    ag.targetPosOrVel.set(fnp.result!!.nearestPos!!)
                }
                replan = true
            }
            if (ag.targetRef == 0L) {
                // Failed to reposition target, fail moverequest.
                ag.corridor.reset(agentRef, agentPos)
                ag.partial = false
                ag.targetState = MoveRequestState.NONE
            }
        }

        // If nearby corridor is not valid, replan.
        if (!ag.corridor.isValid(config.checkLookAhead, navQuery, filters[ag.params.queryFilterType])) {
            // Fix current path.
            // ag.corridor.trimInvalidPath(agentRef, agentPos, m_navquery, &m_filter);
            // ag.boundary.reset();
            replan = true
        }

        // If the end of the path is near and it is not the requested
        // location, replan.
        if (ag.targetState == MoveRequestState.VALID &&
            ag.targetReplanTime > config.targetReplanDelay &&
            ag.corridor.path.size < config.checkLookAhead &&
            ag.corridor.lastPoly != ag.targetRef
        ) replan = true

        // Try to replan path to goal.
        if (replan && ag.targetState != MoveRequestState.NONE) {
            requestMoveTargetReplan(ag, ag.targetRef, ag.targetPosOrVel)
        }
    }

    private fun updateMoveRequest(agents: Collection<CrowdAgent>, dt: Float) {
        telemetry.start(CrowdTelemetryType.UPDATE_MOVE_REQUEST)
        val queue = PriorityQueue<CrowdAgent> { a1, a2 ->
            a2.targetReplanTime.compareTo(a1.targetReplanTime)
        }

        // Fire off new requests.
        for (ag in agents) {
            if (ag.state == CrowdAgentState.INVALID ||
                ag.targetState == MoveRequestState.NONE ||
                ag.targetState == MoveRequestState.VELOCITY
            ) continue

            if (ag.targetState == MoveRequestState.REQUESTING) {
                findPath(ag)
            }
            if (ag.targetState == MoveRequestState.WAITING_FOR_QUEUE) {
                queue.add(ag)
            }
        }
        while (!queue.isEmpty()) {
            val ag = queue.poll()
            ag.targetPathQueryResult = pathQueue.request(
                ag.corridor.lastPoly, ag.targetRef, ag.corridor.target,
                ag.targetPosOrVel, filters[ag.params.queryFilterType]
            )
            if (ag.targetPathQueryResult != null) {
                ag.targetState = MoveRequestState.WAITING_FOR_PATH
            } else {
                telemetry.recordMaxTimeToEnqueueRequest(ag.targetReplanWaitTime)
                ag.targetReplanWaitTime += dt
            }
        }

        // Update requests.
        telemetry.start(CrowdTelemetryType.PATH_QUEUE_UPDATE)
        pathQueue.update(navMesh)
        telemetry.stop(CrowdTelemetryType.PATH_QUEUE_UPDATE)

        // Process path results.
        for (ag in agents) {
            if (ag.targetState == MoveRequestState.NONE ||
                ag.targetState == MoveRequestState.VELOCITY
            ) continue

            if (ag.targetState == MoveRequestState.WAITING_FOR_PATH) {
                processPathResult(ag, dt)
            }
        }
        telemetry.stop(CrowdTelemetryType.UPDATE_MOVE_REQUEST)
    }

    private fun findPath(ag: CrowdAgent) {
        val path = ag.corridor.path
        path.clear() // should be empty anyway

        // Quick search towards the goal.
        navQuery.initSlicedFindPath(
            path[0], ag.targetRef, ag.currentPosition, ag.targetPosOrVel,
            filters[ag.params.queryFilterType], 0
        )
        navQuery.updateSlicedFindPath(config.maxTargetFindPathIterations)
        val pathFound = if (ag.targetReplan) { // && npath > 10)
            // Try to use existing steady path during replan if possible.
            navQuery.finalizeSlicedFindPathPartial(path)
        } else {
            // Try to move towards target when goal changes.
            navQuery.finalizeSlicedFindPath()
        }
        var reqPath = pathFound.result
        var reqPos = Vector3f()
        if (pathFound.succeeded() && reqPath!!.size > 0) {
            // In progress or succeed.
            if (reqPath[reqPath.size - 1] != ag.targetRef) {
                // Partial path, constrain target position inside the
                // last polygon.
                val cr = navQuery.closestPointOnPoly(
                    reqPath[reqPath.size - 1],
                    ag.targetPosOrVel
                )
                if (cr != null) {
                    reqPos = cr.pos
                } else {
                    reqPath = LongArrayList()
                }
            } else {
                reqPos.set(ag.targetPosOrVel)
            }
        } else {
            // Could not find path, start the request from current
            // location.
            reqPos.set(ag.currentPosition)
            reqPath = LongArrayList()
            reqPath.add(path[0])
        }
        ag.corridor.setCorridor(reqPos, reqPath)
        ag.boundary.reset()
        ag.partial = false
        if (reqPath[reqPath.size - 1] == ag.targetRef) {
            ag.targetState = MoveRequestState.VALID
            ag.targetReplanTime = 0f
        } else {
            // The path is longer or potentially unreachable, full plan.
            ag.targetState = MoveRequestState.WAITING_FOR_QUEUE
        }
        ag.targetReplanWaitTime = 0f
    }

    private fun processPathResult(ag: CrowdAgent, dt: Float) {
        // telemetry.recordPathWaitTime(ag.targetReplanTime);
        // Poll path queue.
        val status = ag.targetPathQueryResult!!.status
        if (status != Status.NULL && status.isFailed) {
            retryPathFindingEventually(ag)
        } else if (status != Status.NULL && status.isSuccess) {
            val path = ag.corridor.path
            require(!path.isEmpty()) { "Empty path" }

            // Apply results.
            var targetPos = ag.targetPosOrVel
            var valid = true
            var res = ag.targetPathQueryResult!!.path
            if (res.isEmpty()) {
                valid = false
            }
            ag.partial = status.isPartial

            // Merge result and existing path.
            // The agent might have moved whilst the request is
            // being processed, so the path may have changed.
            // We assume that the end of the path is at the same
            // location
            // where the request was issued.

            // The last ref in the old path should be the same as
            // the location where the request was issued..
            if (valid && path[path.size - 1] != res[0]) {
                valid = false
            }
            if (valid) {
                // Put the old path infront of the old path.
                if (path.size > 1) {
                    path.remove(path.size - 1)
                    path.addAll(res)
                    res = path
                    // Remove trackbacks
                    var j = 1
                    while (j < res.size - 1) {
                        if (j - 1 >= 0 && j + 1 < res.size) {
                            if (res[j - 1] == res[j + 1]) {
                                res.remove(j + 1)
                                res.remove(j)
                                j -= 2
                            }
                        }
                        ++j
                    }
                }

                // Check for partial path.
                if (res[res.size - 1] != ag.targetRef) {
                    // Partial path, constrain target position inside
                    // the last polygon.
                    val cr = navQuery.closestPointOnPoly(res[res.size - 1], targetPos)
                    if (cr != null) {
                        targetPos = cr.pos
                    } else {
                        valid = false
                    }
                }
            }
            if (valid) {
                // Set current corridor.
                ag.corridor.setCorridor(targetPos, res)
                // Force to update boundary.
                ag.boundary.reset()
                ag.targetState = MoveRequestState.VALID
            } else {
                // Something went wrong.
                ag.targetState = MoveRequestState.FAILED
            }
            ag.targetReplanTime = 0f
        }
        telemetry.recordMaxTimeToFindPath(ag.targetReplanWaitTime)
        ag.targetReplanWaitTime += dt
    }

    /**
     * Path find failed, retry if the target location is still valid.
     * */
    private fun retryPathFindingEventually(ag: CrowdAgent) {
        ag.targetPathQueryResult = null
        if (ag.targetRef != 0L) {
            ag.targetState = MoveRequestState.REQUESTING
        } else {
            ag.targetState = MoveRequestState.FAILED
        }
        ag.targetReplanTime = 0f
    }

    private fun updateTopologyOptimization(agents: Collection<CrowdAgent>, dt: Float) {
        telemetry.start(CrowdTelemetryType.UPDATE_TOPOLOGY_OPTIMIZATION)
        val queue = PriorityQueue { a1: CrowdAgent, a2: CrowdAgent ->
            a2.topologyOptTime.compareTo(a1.topologyOptTime)
        }
        for (ag in agents) {
            if (ag.state != CrowdAgentState.WALKING ||
                ag.targetState == MoveRequestState.NONE ||
                ag.targetState == MoveRequestState.VELOCITY ||
                ag.params.updateFlags and CrowdAgentParams.CROWD_OPTIMIZE_TOPO == 0
            ) continue
            ag.topologyOptTime += dt
            if (ag.topologyOptTime >= config.topologyOptimizationTimeThreshold) {
                queue.add(ag)
            }
        }
        while (!queue.isEmpty()) {
            val ag = queue.poll()
            ag.corridor.optimizePathTopology(
                navQuery,
                filters[ag.params.queryFilterType],
                config.maxTopologyOptimizationIterations
            )
            ag.topologyOptTime = 0f
        }
        telemetry.stop(CrowdTelemetryType.UPDATE_TOPOLOGY_OPTIMIZATION)
    }

    private fun buildProximityGrid(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.BUILD_PROXIMITY_GRID)
        grid.clear()
        for (ag in agents) {
            val p = ag.currentPosition
            val r = ag.params.radius
            grid.addAgentToBuckets(ag, p.x - r, p.z - r, p.x + r, p.z + r)
        }
        telemetry.stop(CrowdTelemetryType.BUILD_PROXIMITY_GRID)
    }

    private val tinyNodePool = NodePool()
    private val tmpSegments = ArrayList<FloatArray>()
    private val tmpInts = ArrayList<NavMeshQuery.SegInterval>()
    private val tmpPortal = NavMeshQuery.PortalResult()
    private val tmpN = Vector3f()
    private val tmpStack = LinkedList<Node>()
    private fun buildNeighbours(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.BUILD_NEIGHBORS)
        for (agent in agents) {
            buildNeighbours(agent)
        }
        telemetry.stop(CrowdTelemetryType.BUILD_NEIGHBORS)
    }

    private fun buildNeighbours(ag: CrowdAgent) {
        if (ag.state != CrowdAgentState.WALKING) return

        // Update the collision boundary after certain distance has been passed or
        // if it has become invalid.
        val updateThr = ag.params.collisionQueryRange * 0.25f
        if (Vectors.dist2DSqr(ag.currentPosition, ag.boundary.center) > updateThr * updateThr
            || !ag.boundary.isValid(navQuery, filters[ag.params.queryFilterType])
        ) {
            ag.boundary.update(
                ag.corridor.firstPoly, ag.currentPosition, ag.params.collisionQueryRange, navQuery,
                filters[ag.params.queryFilterType], tinyNodePool, tmpVertices, tmpVertices2,
                tmpSegments, tmpInts, tmpPortal, tmpN, tmpStack
            )
        }
        // Query neighbour agents
        getNeighbours(ag, ag.params.collisionQueryRange, grid, ag.neis)
    }

    private val tmpAgents = ArrayList<CrowdAgent>()
    private fun getNeighbours(
        self: CrowdAgent,
        range: Float,
        grid: ProximityGrid,
        dst: ArrayList<CrowdNeighbour>
    ) {
        synchronized(neighbourCache) {
            neighbourCache.addAll(dst)
        }
        dst.clear()
        val pos = self.currentPosition
        val agents = tmpAgents
        grid.queryItems(
            pos.x - range, pos.z - range,
            pos.x + range, pos.z + range,
            self, range, agents
        )
        for (i in agents.indices) {
            val ag = agents[i]
            // Check for overlap.
            val cp = ag.currentPosition
            val dx = pos.x - cp.x
            val dz = pos.z - cp.z
            val distSqr = dx * dx + dz * dz
            dst.add(createNeighbour(ag, distSqr))
        }
        dst.sortWith { a, b ->
            a.dist.compareTo(b.dist)
        }
    }

    private val tmp = NavMeshQuery.PortalResult()
    private fun findCorners(agents: Collection<CrowdAgent>, debug: CrowdAgentDebugInfo?) {
        telemetry.start(CrowdTelemetryType.FIND_CORNERS)
        for (ag in agents) {
            findCorners(ag, debug)
        }
        telemetry.stop(CrowdTelemetryType.FIND_CORNERS)
    }

    private fun findCorners(ag: CrowdAgent, debug: CrowdAgentDebugInfo?) {

        if (ag.state != CrowdAgentState.WALKING ||
            ag.targetState == MoveRequestState.NONE ||
            ag.targetState == MoveRequestState.VELOCITY
        ) return

        val debugAgent = debug?.agent
        // Find corners for steering
        StraightPathItem.clear(ag.corners)
        ag.corners.addAll(
            ag.corridor.findCorners(
                CROWDAGENT_MAX_CORNERS, navQuery, tmp,
                tmpVertices, tmpVertices2, tmpVertices3
            )
        )

        // Check to see if the corner after the next corner is directly visible,
        // and short-cut to there.
        if (ag.params.updateFlags and CrowdAgentParams.CROWD_OPTIMIZE_VIS != 0 && ag.corners.size > 0) {
            val target = ag.corners[min(1, ag.corners.size - 1)].pos
            ag.corridor.optimizePathVisibility(
                target, ag.params.pathOptimizationRange, navQuery,
                filters[ag.params.queryFilterType]
            )

            // Copy data for debug purposes.
            if (debugAgent === ag) {
                debug.optStart.set(ag.corridor.pos)
                debug.optEnd.set(target)
            }
        } else {
            // Copy data for debug purposes.
            if (debugAgent === ag) {
                debug.optStart.set(0f)
                debug.optEnd.set(0f)
            }
        }
    }

    private fun triggerOffMeshConnections(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.TRIGGER_OFF_MESH_CONNECTIONS)
        for (agent in agents) {
            triggerOffMeshConnections(agent)
        }
        telemetry.stop(CrowdTelemetryType.TRIGGER_OFF_MESH_CONNECTIONS)
    }

    private fun triggerOffMeshConnections(ag: CrowdAgent) {

        if (ag.state != CrowdAgentState.WALKING ||
            ag.targetState == MoveRequestState.NONE ||
            ag.targetState == MoveRequestState.VELOCITY
        ) return

        // Check
        val triggerRadius = ag.params.radius * 2.25f
        if (!ag.overOffmeshConnection(triggerRadius)) return

        // Prepare to off-mesh connection.
        val anim = ag.animation

        // Adjust the path over the off-mesh connection.
        val refs = LongArray(2)
        if (ag.corridor.moveOverOffmeshConnection(
                ag.corners[ag.corners.size - 1].ref, refs,
                anim.startPos, anim.endPos, navQuery
            )
        ) {
            anim.initPos.set(ag.currentPosition)
            anim.polyRef = refs[1]
            anim.active = true
            anim.t = 0f
            anim.tMax = Vectors.dist2D(anim.startPos, anim.endPos) / ag.params.maxSpeed * 0.5f
            ag.state = CrowdAgentState.OFFMESH
            StraightPathItem.clear(ag.corners)
            synchronized(neighbourCache) {
                neighbourCache.addAll(ag.neis)
            }
            ag.neis.clear()
        }
        // else Path validity check will ensure that bad/blocked connections will be replanned.
    }

    private fun calculateSteering(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.CALCULATE_STEERING)
        for (agent in agents) {
            calculateSteering(agent)
        }
        telemetry.stop(CrowdTelemetryType.CALCULATE_STEERING)
    }

    private fun calculateSteering(ag: CrowdAgent) {
        if (ag.state != CrowdAgentState.WALKING || ag.targetState == MoveRequestState.NONE) {
            return
        }

        val dvel = ag.desiredVelocity
        if (ag.targetState == MoveRequestState.VELOCITY) {
            dvel.set(ag.targetPosOrVel)
            ag.desiredSpeed = ag.targetPosOrVel.length()
        } else {
            // Calculate steering direction.
            if (ag.params.updateFlags and CrowdAgentParams.CROWD_ANTICIPATE_TURNS != 0) {
                ag.calcSmoothSteerDirection(dvel)
            } else {
                ag.calcStraightSteerDirection(dvel)
            }
            // Calculate speed scale, which tells the agent to slowdown at the end of the path.
            val slowDownRadius = ag.params.radius * 2 // TODO: make less hacky.
            val speedScale = ag.getDistanceToGoal(slowDownRadius) / slowDownRadius
            ag.desiredSpeed = ag.params.maxSpeed
            dvel.mul(ag.desiredSpeed * speedScale)
        }

        // Separation
        if (ag.params.updateFlags and CrowdAgentParams.CROWD_SEPARATION != 0) {
            val separationDist = ag.params.collisionQueryRange
            val invSeparationDist = 1f / separationDist
            val separationWeight = ag.params.separationWeight
            var w = 0f
            var dispX = 0f
            var dispZ = 0f
            for (j in ag.neis.indices) {
                val nei = ag.neis[j].agent
                val diffX = ag.currentPosition.x - nei.currentPosition.x
                val diffZ = ag.currentPosition.z - nei.currentPosition.z
                val distSqr = diffX * diffX + diffZ * diffZ
                if (distSqr < 0.00001f || distSqr > separationDist * separationDist) {
                    continue
                }
                val dist = sqrt(distSqr)
                val weight = separationWeight * (1f - Vectors.sq(dist * invSeparationDist))
                val f = weight / dist
                dispX += diffX * f
                dispZ += diffZ * f
                w += 1f
            }
            if (w > 0.0001f) {
                // Adjust desired velocity.
                val f = 1f / w
                dvel.add(dispX * f, 0f, dispZ * f)
                // Clamp desired velocity to desired speed.
                val speedSqr = dvel.lengthSquared()
                val desiredSqr = ag.desiredSpeed * ag.desiredSpeed
                if (speedSqr > desiredSqr) {
                    dvel.mul(desiredSqr / speedSqr)
                }
            }
        }
    }

    private fun planVelocity(debug: CrowdAgentDebugInfo?, agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.PLAN_VELOCITY)
        val debugAgent = debug?.agent
        for (ag in agents) {
            if (ag.state != CrowdAgentState.WALKING) {
                continue
            }
            if (ag.params.updateFlags and CrowdAgentParams.CROWD_OBSTACLE_AVOIDANCE != 0) {
                obstacleAvoidanceQuery.reset()

                // Add neighbours as obstacles.
                for (j in ag.neis.indices) {
                    val nei = ag.neis[j].agent
                    obstacleAvoidanceQuery.addCircle(
                        nei.currentPosition,
                        nei.params.radius,
                        nei.actualVelocity,
                        nei.desiredVelocity
                    )
                }

                // Append neighbour segments as obstacles.
                for (j in ag.boundary.segments.indices) {
                    val s = ag.boundary.segments[j]
                    if (Vectors.triArea2D(ag.currentPosition, s.start, s.end) < 0f) continue
                    obstacleAvoidanceQuery.addSegment(s.start, s.end)
                }

                var debugData: ObstacleAvoidanceDebugData? =
                    if (debugAgent === ag) debug.debugData else null

                // Sample new safe velocity.
                val params = obstacleQueryParams[ag.params.obstacleAvoidanceType]
                val (first, second) = obstacleAvoidanceQuery.sampleVelocityAdaptive(
                    ag.currentPosition, ag.params.radius, ag.desiredSpeed, ag.actualVelocity, ag.desiredVelocity,
                    params, debugData
                )
                ag.desiredVelAdjusted.set(second)
                velocitySampleCount += first
            } else {
                // If not using velocity planning, new velocity is directly the desired velocity.
                ag.desiredVelAdjusted.set(ag.desiredVelocity)
            }
        }
        telemetry.stop(CrowdTelemetryType.PLAN_VELOCITY)
    }

    private fun integrate(dt: Float, agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.INTEGRATE)
        for (ag in agents) {
            if (ag.state != CrowdAgentState.WALKING) continue
            ag.integrate(dt)
        }
        telemetry.stop(CrowdTelemetryType.INTEGRATE)
    }

    private fun handleCollisions(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.HANDLE_COLLISIONS)
        for (iter in 0..3) {
            for (ag in agents) {
                handleCollisions(ag)
            }
            for (ag in agents) {
                if (ag.state != CrowdAgentState.WALKING) continue
                ag.currentPosition.add(ag.disp)
            }
        }
        telemetry.stop(CrowdTelemetryType.HANDLE_COLLISIONS)
    }

    private fun handleCollisions(ag: CrowdAgent) {
        if (ag.state != CrowdAgentState.WALKING) return
        val idx0 = ag.idx

        val disp = ag.disp.set(0f)
        var w = 0f
        for (j in ag.neis.indices) {
            val nei = ag.neis[j].agent
            val idx1 = nei.idx
            val p0 = ag.currentPosition
            val p1 = nei.currentPosition
            var dx = p0.x - p1.x
            var dz = p0.z - p1.z

            var dist = dx * dx + dz * dz
            if (dist > Vectors.sq(ag.params.radius + nei.params.radius)) {
                continue
            }

            dist = sqrt(dist)
            var pen = ag.params.radius + nei.params.radius - dist
            pen = if (dist < 0.0001f) {
                // Agents on top of each other, try to choose diverging separation directions.
                if (idx0 > idx1) {
                    dx = -ag.desiredVelocity.z
                    dz = ag.desiredVelocity.x
                } else {
                    dx = ag.desiredVelocity.z
                    dz = -ag.desiredVelocity.x
                }
                0.01f
            } else {
                (pen * 0.5f) * config.collisionResolveFactor / dist
            }
            disp.add(dx * pen, 0f, dz * pen)
            w += 1f
        }
        if (w > 0.0001f) {
            val iw = 1f / w
            disp.mul(iw)
        }
    }

    private val tmpVertices = FloatArray(nav.maxVerticesPerPoly * 3)
    private val tmpVertices2 = FloatArray(nav.maxVerticesPerPoly * 3)
    private val tmpVertices3 = FloatArray(nav.maxVerticesPerPoly)
    private val tmpNeis = LongArray(MAX_NEIS)
    private val tmpVisited = LongArrayList()

    private fun moveAgents(agents: Collection<CrowdAgent>) {
        telemetry.start(CrowdTelemetryType.MOVE_AGENTS)
        for (agent in agents) {
            moveAgent(agent)
        }
        telemetry.stop(CrowdTelemetryType.MOVE_AGENTS)
    }

    private fun moveAgent(ag: CrowdAgent) {
        if (ag.state != CrowdAgentState.WALKING) return

        // Move along navmesh.
        ag.corridor.movePosition(
            ag.currentPosition, navQuery, filters[ag.params.queryFilterType],
            tinyNodePool, tmpVertices, tmpNeis, tmpVisited
        )
        // Get valid constrained position back.
        ag.currentPosition.set(ag.corridor.pos)

        // If not using path, truncate the corridor to just one poly.
        if (ag.targetState == MoveRequestState.NONE
            || ag.targetState == MoveRequestState.VELOCITY
        ) {
            ag.corridor.reset(ag.corridor.firstPoly, ag.currentPosition)
            ag.partial = false
        }
    }

    private fun updateOffMeshConnections(agents: Collection<CrowdAgent>, dt: Float) {
        telemetry.start(CrowdTelemetryType.UPDATE_OFF_MESH_CONNECTIONS)
        for (agent in agents) {
            updateOffMeshConnections(agent, dt)
        }
        telemetry.stop(CrowdTelemetryType.UPDATE_OFF_MESH_CONNECTIONS)
    }

    private fun updateOffMeshConnections(ag: CrowdAgent, dt: Float) {
        val anim = ag.animation
        if (!anim.active) {
            return
        }
        anim.t += dt
        if (anim.t > anim.tMax) {
            // Reset animation
            anim.active = false
            // Prepare agent for walking.
            ag.state = CrowdAgentState.WALKING
            return
        }

        // Update position
        val ta = anim.tMax * 0.15f
        val tb = anim.tMax
        if (anim.t < ta) {
            val u = tween(anim.t, 0f, ta)
            anim.initPos.lerp(anim.startPos, u, ag.currentPosition)
        } else {
            val u = tween(anim.t, ta, tb)
            anim.startPos.lerp(anim.endPos, u, ag.currentPosition)
        }

        // Update velocity.
        ag.actualVelocity.set(0f)
        ag.desiredVelocity.set(0f)
    }

    private fun tween(t: Float, t0: Float, t1: Float): Float {
        return Vectors.clamp((t - t0) / (t1 - t0), 0f, 1f)
    }

    /**
     * Provides neighbor data for agents managed by the crowd.
     */
    class CrowdNeighbour(
        /**
         * The index of the neighbor in the crowd.
         */
        var agent: CrowdAgent,
        /**
         * The distance between the current agent and the neighbor.
         */
        var dist: Float
    )

    companion object {
        /** The maximum number of corners a crowd agent will look ahead in the path.
         * This value is used for sizing the crowd agent corner buffers.
         * Due to the behavior of the crowd manager, the actual number of useful
         * corners will be one less than this number.
         * */
        const val CROWDAGENT_MAX_CORNERS = 4

        /**
         * The maximum number of crowd avoidance configurations supported by the crowd manager.
         * @see dtObstacleAvoidanceParams, dtCrowd::setObstacleAvoidanceParams(), dtCrowd::getObstacleAvoidanceParams(),dtCrowdAgentParams::obstacleAvoidanceType
         * */
        const val CROWD_MAX_OBSTAVOIDANCE_PARAMS = 8

        /**
         * The maximum number of query filter types supported by the crowd manager.
         * @see dtQueryFilter, dtCrowd::getFilter() dtCrowd::getEditableFilter(), dtCrowdAgentParams::queryFilterType
         * */
        const val CROWD_MAX_QUERY_FILTER_TYPE = 16

        private val neighbourCache = ArrayList<CrowdNeighbour>()
        fun createNeighbour(agent: CrowdAgent, dist: Float): CrowdNeighbour {
            return synchronized(neighbourCache) {
                val nei = neighbourCache.removeLastOrNull()
                if (nei == null) CrowdNeighbour(agent, dist)
                else {
                    nei.agent = agent
                    nei.dist = dist
                    nei
                }
            }
        }

    }
}