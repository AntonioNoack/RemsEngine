package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import kotlin.math.ceil
import kotlin.math.max

internal abstract class AbstractGroundSampler : GroundSampler {

    protected fun sampleGround(
        cfg: JumpLinkBuilderConfig, es: EdgeSampler,
        heightFunc: (Vector3f, Float) -> Pair<Boolean, Float>
    ) {
        val cellSize = cfg.cellSize
        val dist = es.start.p.distance(es.start.q)
        val numSamples = max(2, ceil((dist / cellSize)).toInt())
        sampleGroundSegment(heightFunc, es.start, numSamples)
        for (end in es.end) {
            sampleGroundSegment(heightFunc, end, numSamples)
        }
    }

    private fun sampleGroundSegment(
        heightFunc: (Vector3f, Float) -> Pair<Boolean, Float>,
        seg: GroundSegment,
        numSamples: Int
    ) {
        seg.samples = Array(numSamples) { GroundSample() }
        for (i in 0 until numSamples) {
            val u = i / (numSamples - 1).toFloat()
            val s = seg.samples!![i]
            seg.p.lerp(seg.q, u, s.position)
            val (first, second) = heightFunc(s.position, seg.height)
            s.position.y = second
            if (!first) continue
            s.validHeight = true
        }
    }
}