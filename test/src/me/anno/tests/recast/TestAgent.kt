package me.anno.tests.recast

import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.recast.NavMeshAgent
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshUtils
import me.anno.tests.recast.RecastTests.Companion.enableDrawing
import me.anno.utils.Color.black
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f

class TestAgent(
    data: NavMeshData,
    val start: Vector3f,
    val target: Vector3f
) : NavMeshAgent(data) {

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        dstUnion.all()
        return true
    }

    override fun onUpdate() {
        if (crowdAgent == null) {
            super.onUpdate()
            moveTo(target)
        }

        if (enableDrawing) {
            // draw current position
            val pos = crowdAgent!!.currentPosition
            DebugShapes.debugPoints.add(DebugPoint(Vector3d(pos), 0x2277ff or black, 0f))
            // draw current path
            val c = crowdAgent!!.corridor
            NavMeshUtils.drawPath(data.navMesh, data.meshData, c.path, 0x555555 or black)
        }
    }
}