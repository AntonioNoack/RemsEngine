package me.anno.games.roadcraft

import com.bulletphysics.collision.shapes.HeightMapShape
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.engine.ui.LineShapes.getDrawMatrix
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d
import org.joml.Vector3f

class TerrainCollider(
    val sizeX: Int, val sizeZ: Int,
    val minHeight: Float, val maxHeight: Float,
    val heightScale: Float, val heightOffset: Float,
    val unsigned: Boolean, val heightData: ShortArray
) : Collider(), CustomBulletCollider {

    override fun createBulletCollider(scale: Vector3f): HeightMapShape {
        val shape = HeightMapShape()
        shape.upAxis = Axis.Y
        shape.sizeI = sizeX
        shape.sizeJ = sizeZ
        shape.minHeight = minHeight.toDouble()
        shape.maxHeight = maxHeight.toDouble()
        shape.heightScale = heightScale.toDouble()
        shape.heightOffset = heightOffset.toDouble()
        shape.unsigned = unsigned
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
        // tmp variables are necessary, otherwise we might transform a vertex twice
        val ga = JomlPools.vec3d.create()
        val gb = JomlPools.vec3d.create()
        val gc = JomlPools.vec3d.create()
        shape.processAllTriangles({ a, b, c, _, _ ->
            if (transform != null) {
                transform.transformPosition(a, ga)
                transform.transformPosition(b, gb)
                transform.transformPosition(c, gc)
                LineBuffer.addLine(ga, gb, color)
                LineBuffer.addLine(gb, gc, color)
                LineBuffer.addLine(gc, ga, color)
            } else {
                LineBuffer.addLine(a, b, color)
                LineBuffer.addLine(b, c, color)
                LineBuffer.addLine(c, a, color)
            }
        }, ga.set(-1e308), gb.set(1e308))
        JomlPools.vec3d.sub(3)
    }
}
