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

/** Configuration parameters for a crowd agent.  */
class CrowdAgentParams {

    /**
     * >= 0
     * */
    var radius = 0f

    /**
     * > 0
     * */
    var height = 1f

    /** Maximum allowed acceleration. >= 0  */
    var maxAcceleration = 0f

    /** Maximum allowed speed. >= 0  */
    var maxSpeed = 0f

    /** Defines how close a collision element must be before it is considered for steering behaviors. > 0  */
    var collisionQueryRange = 0f

    /** The path visibility optimization range. > 0  */
    var pathOptimizationRange = 0f

    /** How aggressive the agent manager should be at avoiding collisions with this agent. >= 0  */
    var separationWeight = 0f

    /** Flags that impact steering behavior. (See: #UpdateFlags)  */
    var updateFlags = 0

    /**
     * The index of the avoidance configuration to use for the agent.
     * [Limits: 0 <= value < #CROWD_MAX_OBSTAVOIDANCE_PARAMS]
     */
    var obstacleAvoidanceType = 0

    /** The index of the query filter used by this agent.  */
    var queryFilterType = 0

    /** User defined data attached to the agent.  */
    var userData: Any? = null

    companion object {
        /** Crowd agent update flags.  */
        const val CROWD_ANTICIPATE_TURNS = 1
        const val CROWD_OBSTACLE_AVOIDANCE = 2
        const val CROWD_SEPARATION = 4

        /** Use #dtPathCorridor::optimizePathVisibility() to optimize the agent path.  */
        const val CROWD_OPTIMIZE_VIS = 8

        /** Use dtPathCorridor::optimizePathTopology() to optimize the agent path.  */
        const val CROWD_OPTIMIZE_TOPO = 16
    }
}