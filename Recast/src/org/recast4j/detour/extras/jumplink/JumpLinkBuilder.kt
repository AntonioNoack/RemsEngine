package org.recast4j.detour.extras.jumplink

import org.recast4j.Vectors
import org.recast4j.recast.RecastBuilder.RecastBuilderResult
import kotlin.math.max
import kotlin.math.min

class JumpLinkBuilder(private val results: List<RecastBuilderResult>) {

    val edges = results.map { EdgeExtractor.extractEdges(it.mesh) }

    fun build(cfg: JumpLinkBuilderConfig, type: JumpLinkType): List<JumpLink> {
        val links = ArrayList<JumpLink>()
        for (tile in results.indices) {
            val edges = edges[tile]
            val result = results[tile]
            for (i in edges.indices) {
                links.addAll(processEdge(cfg, result, type, edges[i]))
            }
        }
        return links
    }

    private fun processEdge(
        cfg: JumpLinkBuilderConfig,
        result: RecastBuilderResult,
        type: JumpLinkType,
        edge: Edge
    ): List<JumpLink> {
        val es = EdgeSamplerFactory[cfg, type, edge]
        NavMeshGroundSampler.sample(cfg, result, es)
        TrajectorySampler.sample(cfg, result.solidHeightField, es)
        val jumpSegments = JumpSegmentBuilder.build(cfg, es)
        return buildJumpLinks(cfg, es, jumpSegments)
    }

    private fun buildJumpLinks(
        cfg: JumpLinkBuilderConfig, es: EdgeSampler,
        jumpSegments: Array<JumpSegment>
    ): List<JumpLink> {
        val minDistance = 4 * cfg.agentRadius * cfg.agentRadius
        return jumpSegments.mapNotNull { buildJumpLink(minDistance, es, it) }
    }

    private fun buildJumpLink(minDistance: Float, es: EdgeSampler, js: JumpSegment): JumpLink? {
        val startSamples = es.start.samples!!
        val sp = startSamples[js.startSample].position
        val sq = startSamples[js.startSample + js.samples - 1].position
        val end = es.end[js.groundSegment]
        val endSamples = end.samples!!
        val ep = endSamples[js.startSample].position
        val eq = endSamples[js.startSample + js.samples - 1].position
        val distance = min(Vectors.dist2DSqr(sp, sq), Vectors.dist2DSqr(ep, eq))
        if (distance.isNaN() || distance < minDistance) return null
        val link = JumpLink()
        link.startSamples = startSamples.copyOfRange(js.startSample, js.startSample + js.samples)
        link.endSamples = endSamples.copyOfRange(js.startSample, js.startSample + js.samples)
        link.start = es.start
        link.end = end
        link.trajectory = es.trajectory
        for (j in 0 until link.numSpines) {
            val t = j.toFloat() / max(1, link.numSpines - 1)
            es.trajectory.apply(sp, ep, t).get(link.spine0, j * 3)
            es.trajectory.apply(sq, eq, t).get(link.spine1, j * 3)
        }
        return link
    }
}