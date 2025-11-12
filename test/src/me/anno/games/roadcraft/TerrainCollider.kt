package me.anno.games.roadcraft

import com.bulletphysics.collision.shapes.HeightMapShape
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.engine.ui.LineShapes.getDrawMatrix
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.pipeline.Pipeline
import org.joml.Vector3d
import org.joml.Vector3f

class TerrainCollider(
    val width: Int, val length: Int,
    val minHeight: Float, val maxHeight: Float,
    val heightData: ShortArray
) : Collider(), CustomBulletCollider {

    override fun createBulletCollider(scale: Vector3f): HeightMapShape {
        val shape = HeightMapShape()
        shape.upAxis = Axis.Y
        shape.width = width
        shape.length = length
        shape.minHeight = minHeight.toDouble()
        shape.maxHeight = maxHeight.toDouble()
        shape.heightData = heightData
        shape.defineBounds()
        shape.localScaling = scale
        return shape
    }

    override fun drawShape(pipeline: Pipeline) {
        // unoptimized implementation, but as a plus, it's very easy;
        val transform = getDrawMatrix(entity)
        val shape = createBulletCollider(Vector3f(1f))
        val color = colliderLineColor
        shape.processAllTriangles({ p0, p1, p2, _, _ ->
            if (transform != null) {
                transform.transformPosition(p0)
                transform.transformPosition(p1)
                transform.transformPosition(p2)
            }
            LineBuffer.addLine(p0, p1, color)
            LineBuffer.addLine(p1, p2, color)
            LineBuffer.addLine(p2, p0, color)
        }, Vector3d(-1e308), Vector3d(1e308))
    }
}
