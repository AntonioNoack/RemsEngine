package me.anno.recast

import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.QueryFilter
import org.recast4j.detour.crowd.Crowd

/**
 * everything needed for a simple agent
 * */
class NavMeshData(
    val meshData: MeshData,
    val navMesh: org.recast4j.detour.NavMesh,
    val query: NavMeshQuery,
    val filter: QueryFilter,
    val agentType: AgentType,
    val crowd: Crowd,
    val collisionMask: Int,
)