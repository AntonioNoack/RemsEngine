/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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

class CrowdConfig(val maxAgentRadius: Float) {
    /**
     * Max number of path requests in the queue
     */
    var pathQueueSize = 32

    /**
     * Max number of sliced path finding iterations executed per update (used to handle longer paths and replans)
     */
    var maxFindPathIterations = 100

    /**
     * Max number of sliced path finding iterations executed per agent to find the initial path to target
     */
    var maxTargetFindPathIterations = 20

    /**
     * Min time between topology optimizations (in seconds)
     */
    var topologyOptimizationTimeThreshold = 0.5f

    /**
     * The number of polygons from the beginning of the corridor to check to ensure path validity
     */
    var checkLookAhead = 10

    /**
     * Min time between target re-planning (in seconds)
     */
    var targetReplanDelay = 1f

    /**
     * Max number of sliced path finding iterations executed per topology optimization per agent
     */
    var maxTopologyOptimizationIterations = 32
    var collisionResolveFactor = 0.7f

    /**
     * Max number of neighbour agents to consider in obstacle avoidance processing
     */
    var maxObstacleAvoidanceCircles = 6

    /**
     * Max number of neighbour segments to consider in obstacle avoidance processing
     */
    var maxObstacleAvoidanceSegments = 8
}