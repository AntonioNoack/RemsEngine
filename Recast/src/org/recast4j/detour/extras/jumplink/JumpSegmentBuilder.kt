package org.recast4j.detour.extras.jumplink

import org.recast4j.IntArray2D
import org.recast4j.IntPair
import kotlin.math.abs

internal object JumpSegmentBuilder {

    fun build(cfg: JumpLinkBuilderConfig, es: EdgeSampler): Array<JumpSegment> {
        val n = es.end[0].samples!!.size
        val sampleGrid = IntArray2D(n, es.end.size)
        for (j in es.end.indices) {
            for (i in 0 until n) {
                sampleGrid[i, j] = -1
            }
        }
        val region = fillConnectedRegions(es, sampleGrid, n, cfg)
        return findLongestSegmentsPerRegion(es, sampleGrid, n, region)
    }

    private fun fillConnectedRegions(
        es: EdgeSampler, sampleGrid: IntArray2D,
        n: Int, cfg: JumpLinkBuilderConfig
    ): Int {
        var region = 0
        val queue = ArrayList<IntPair>()
        for (j in es.end.indices) {
            for (i in 0 until n) {
                if (sampleGrid[i, j] == -1) {
                    val p = es.end[j].samples!![i]
                    if (!p.validTrajectory) {
                        sampleGrid[i, j] = -2
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
        es: EdgeSampler, sampleGrid: IntArray2D,
        n: Int, region: Int,
    ): Array<JumpSegment> {
        val jumpSegments = Array(region) { JumpSegment() }
        for (j in es.end.indices) {
            var l = 0
            var r = -2
            for (i in 0 until n + 1) {
                if (i == n || sampleGrid[i, j] != r) {
                    if (r >= 0) {
                        if (jumpSegments[r].samples < l) {
                            jumpSegments[r].samples = l
                            jumpSegments[r].startSample = i - l
                            jumpSegments[r].groundSegment = j
                        }
                    }
                    if (i < n) {
                        r = sampleGrid[i, j]
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
        sampleGrid: IntArray2D,
        queue: ArrayList<IntPair>,
        agentClimb: Float,
        region: Int
    ) {
        while (true) {
            val (i, j) = queue.removeLastOrNull() ?: break
            if (sampleGrid[i, j] == -1) {
                val p = es.end[j].samples!![i]
                sampleGrid[i, j] = region
                val h = p.position.y
                if (i < sampleGrid.sizeX - 1) {
                    addNeighbour(es, queue, agentClimb, h, i + 1, j)
                }
                if (i > 0) {
                    addNeighbour(es, queue, agentClimb, h, i - 1, j)
                }
                if (j < sampleGrid.sizeY - 1) {
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