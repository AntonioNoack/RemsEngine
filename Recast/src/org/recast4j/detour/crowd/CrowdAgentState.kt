package org.recast4j.detour.crowd

/**
 * The type of navigation mesh polygon the agent is currently traversing.
 */
enum class CrowdAgentState {
    /** The agent is not in a valid state.  */
    INVALID,

    /** The agent is traversing a normal navigation mesh polygon.  */
    WALKING,

    /** The agent is traversing an off-mesh connection.  */
    OFF_MESH
}