package org.recast4j.detour.extras.jumplink

import me.anno.image.raw.IntImage
import me.anno.utils.structures.tuples.IntPair
import kotlin.math.abs

internal object JumpSegmentBuilder {

    fun build(cfg: JumpLinkBuilderConfig, es: EdgeSampler): Array<JumpSegment> {
        val n = es.end[0].samples!!.size
        val sampleGrid = IntImage(n, es.end.size, false)
        sampleGrid.data.fill(-1)
        val region = fillConnectedRegions(es, sampleGrid, n, cfg)
        return findLongestSegmentsPerRegion(es, sampleGrid, n, region)
    }

    private fun fillConnectedRegions(
        es: EdgeSampler, sampleGrid: IntImage,
        n: Int, cfg: JumpLinkBuilderConfig
    ): Int {
        var region = 0
        val queue = ArrayList<IntPair>()
        for (j in es.end.indices) {
            for (i in 0 until n) {
                if (sampleGrid.getRGB(i, j) == -1) {
                    val p = es.end[j].samples!![i]
                    if (!p.validTrajectory) {
                        sampleGrid.setRGB(i, j, -2)
                    } else {
                        queue.clear() // not really necessary
                        queue.add(IntPair(i, j))
                        fill(es, sampleGrid, queue, cfg.agentClimb, region)
                        region++
                    }
                }
            }
        }
        return region
    }

    private fun findLongestSegmentsPerRegion(
        es: EdgeSampler, sampleGrid: IntImage,
        n: Int, region: Int,
    ): Array<JumpSegment> {
        val jumpSegments = Array(region) { JumpSegment() }
        for (j in es.end.indices) {
            var l = 0
            var r = -2
            for (i in 0 until n + 1) {
                if (i == n || sampleGrid.getRGB(i, j) != r) {
                    if (r >= 0) {
                        val segment = jumpSegments[r]
                        if (segment.samples < l) {
                            segment.samples = l
                            segment.startSample = i - l
                            segment.groundSegment = j
                        }
                    }
                    if (i < n) {
                        r = sampleGrid.getRGB(i, j)
                    }
                    l = 1
                } else {
                    l++
                }
            }
        }
        return jumpSegments
    }

    private fun fill(
        es: EdgeSampler,
        sampleGrid: IntImage,
        queue: ArrayList<IntPair>,
        agentClimb: Float,
        region: Int
    ) {
        while (true) {
            val (i, j) = queue.removeLastOrNull() ?: break
            if (sampleGrid.getRGB(i, j) == -1) {
                val p = es.end[j].samples!![i]
                sampleGrid.setRGB(i, j, region)
                val h = p.position.y
                if (i < sampleGrid.width - 1) {
                    addNeighbour(es, queue, agentClimb, h, i + 1, j)
                }
                if (i > 0) {
                    addNeighbour(es, queue, agentClimb, h, i - 1, j)
                }
                if (j < sampleGrid.height - 1) {
                    addNeighbour(es, queue, agentClimb, h, i, j + 1)
                }
                if (j > 0) {
                    addNeighbour(es, queue, agentClimb, h, i, j - 1)
                }
            }
        }
    }

    private fun addNeighbour(es: EdgeSampler, queue: ArrayList<IntPair>, agentClimb: Float, h: Float, i: Int, j: Int) {
        val q = es.end[j].samples!![i]
        if (q.validTrajectory && abs(q.position.y - h) < agentClimb) {
            queue.add(IntPair(i, j))
        }
    }
}