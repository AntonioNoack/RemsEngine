package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import org.recast4j.detour.*
import org.recast4j.recast.RecastBuilderResult
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal object NavMeshGroundSampler : AbstractGroundSampler() {
    private val filter: QueryFilter = NoOpFilter()

    private class NoOpFilter : QueryFilter {

        override fun passFilter(ref: Long, tile: MeshTile?, poly: Poly): Boolean {
            return true
        }

        override fun getCost(
            pa: Vector3f, pb: Vector3f,
            prevRef: Long, prevTile: MeshTile?, prevPoly: Poly?,
            curRef: Long, curTile: MeshTile?, curPoly: Poly,
            nextRef: Long, nextTile: MeshTile?, nextPoly: Poly?
        ): Float = 0f

    }

    override fun sample(cfg: JumpLinkBuilderConfig, result: RecastBuilderResult, es: EdgeSampler) {
        val navMeshQuery = createNavMesh(result, cfg.agentRadius, cfg.agentHeight, cfg.agentClimb)
        sampleGround(cfg, es) { pt: Vector3f, h: Float ->
            getNavMeshHeight(navMeshQuery, pt, cfg.cellSize, h)
        }
    }

    private fun createNavMesh(
        r: RecastBuilderResult,
        agentRadius: Float,
        agentHeight: Float,
        agentClimb: Float
    ): NavMeshQuery {
        val rMesh = r.mesh
        val rMeshDetail = r.meshDetail!!
        val p = NavMeshDataCreateParams()
        p.setFromMesh(rMesh)
        p.setFromMeshDetails(rMeshDetail)
        p.walkableRadius = agentRadius
        p.walkableHeight = agentHeight
        p.walkableClimb = agentClimb
        p.buildBvTree = true
        return NavMeshQuery(NavMesh(NavMeshBuilder.createNavMeshData(p)!!, p.maxVerticesPerPolygon, 0))
    }

    private fun getNavMeshHeight(
        navMeshQuery: NavMeshQuery,
        pt: Vector3f,
        cellSize: Float,
        heightRange: Float
    ): Pair<Boolean, Float> {
        val halfExtents = Vector3f(cellSize, heightRange, cellSize)
        val maxHeight = pt.y + heightRange
        val found = AtomicBoolean()
        val minHeight = AtomicReference(pt.y)
        navMeshQuery.queryPolygons(pt, halfExtents, filter, object : PolyQuery {
            override fun process(tile: MeshTile, poly: Poly, ref: Long) {
                val y = navMeshQuery.getPolyHeight(ref, pt)
                if (y.isFinite() && y > minHeight.get() && y < maxHeight) {
                    minHeight.set(y)
                    found.set(true)
                }
            }
        })
        return if (found.get()) {
            Pair(true, minHeight.get())
        } else Pair(false, pt.y)
    }
}