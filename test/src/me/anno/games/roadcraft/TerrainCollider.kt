package me.anno.games.roadcraft

import com.bulletphysics.collision.shapes.HeightMapShape
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.engine.ui.LineShapes.getDrawMatrix
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.ui.UIColors
import me.anno.utils.Color.withAlpha
import org.joml.Vector3d

class TerrainCollider(
    val width: Int, val length: Int,
    val minHeight: Float, val maxHeight: Float,
    val heightData: ShortArray
) : Collider(), CustomBulletCollider {

    override fun createBulletCollider(scale: Vector3d): HeightMapShape {
        val shape = HeightMapShape()
        shape.upAxis = Axis.Y
        shape.width = width
        shape.length = length
        shape.minHeight = minHeight.toDouble()
        shape.maxHeight = maxHeight.toDouble()
        shape.heightData = heightData
        shape.defineBounds()
        shape.setLocalScaling(scale)
        return shape
    }

    override fun drawShape(pipeline: Pipeline) {
        // unoptimized implementation, but as a plus, it's very easy;
        val transform = getDrawMatrix(entity)
        val shape = createBulletCollider(Vector3d(1.0))
        val color = colliderLineColor
        shape.processAllTriangles({ triangle, _, _ ->
            val p0 = triangle[0]
            val p1 = triangle[1]
            val p2 = triangle[2]
            if (transform != null) {
                transform.transformPosition(p0)
                transform.transformPosition(p1)
                transform.transformPosition(p2)
            }
            LineBuffer.putRelativeLine(p0, p1, color)
            LineBuffer.putRelativeLine(p1, p2, color)
            LineBuffer.putRelativeLine(p2, p0, color)
        }, Vector3d(-1e308), Vector3d(1e308))
    }
}
