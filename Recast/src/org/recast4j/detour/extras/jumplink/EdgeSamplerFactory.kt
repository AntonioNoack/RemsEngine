package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import org.recast4j.Vectors
import kotlin.math.ceil
import kotlin.math.max

internal object EdgeSamplerFactory {
    operator fun get(cfg: JumpLinkBuilderConfig, type: JumpLinkType, edge: Edge): EdgeSampler {
        return when (type) {
            JumpLinkType.EDGE_JUMP -> initEdgeJumpSampler(cfg, edge)
            JumpLinkType.EDGE_CLIMB_DOWN -> initClimbDownSampler(cfg, edge)
            JumpLinkType.EDGE_JUMP_OVER -> throw IllegalArgumentException("Unsupported jump type $type")
        }
    }

    private fun initEdgeJumpSampler(cfg: JumpLinkBuilderConfig, edge: Edge): EdgeSampler {
        val es = EdgeSampler(edge, JumpTrajectory(cfg.jumpHeight))
        es.start.height = cfg.agentClimb * 2
        val offset = Vector3f()
        trans2d(offset, es.az, es.ay, cfg.startDistance, -cfg.agentClimb)
        edge.a.add(offset, es.start.p)
        edge.b.add(offset, es.start.q)
        val dx = cfg.endDistance - 2 * cfg.agentRadius
        val cs = cfg.cellSize
        val numSamples = max(2, ceil((dx / cs)).toInt())
        for (j in 0 until numSamples) {
            val v = j.toFloat() / (numSamples - 1).toFloat()
            val ox = 2 * cfg.agentRadius + dx * v
            trans2d(offset, es.az, es.ay, ox, cfg.minHeight)
            val end = GroundSegment()
            end.height = cfg.heightRange
            edge.a.add(offset, end.p)
            edge.b.add(offset, end.q)
            es.end.add(end)
        }
        return es
    }

    private fun initClimbDownSampler(cfg: JumpLinkBuilderConfig, edge: Edge): EdgeSampler {
        val es = EdgeSampler(edge, ClimbTrajectory)
        es.start.height = cfg.agentClimb * 2
        val offset = Vector3f()
        trans2d(offset, es.az, es.ay, cfg.startDistance, -cfg.agentClimb)
        edge.a.add(offset, es.start.p)
        edge.b.add(offset, es.start.q)
        trans2d(offset, es.az, es.ay, cfg.endDistance, cfg.minHeight)
        val end = GroundSegment()
        end.height = cfg.heightRange
        edge.a.add(offset, end.p)
        edge.b.add(offset, end.q)
        es.end.add(end)
        return es
    }

    private fun trans2d(dst: Vector3f, ax: Vector3f, ay: Vector3f, pt0: Float, pt1: Float) {
        dst.x = ax.x * pt0 + ay.x * pt1
        dst.y = ax.y * pt0 + ay.y * pt1
        dst.z = ax.z * pt0 + ay.z * pt1
    }
}