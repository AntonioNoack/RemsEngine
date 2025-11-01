package me.anno.bullet

import com.bulletphysics.linearmath.DebugDrawModes.NO_DEACTIVATION
import com.bulletphysics.linearmath.IDebugDraw
import me.anno.Build
import me.anno.engine.ui.LineShapes
import me.anno.gpu.buffer.LineBuffer
import me.anno.utils.Color.rgb
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

object BulletDebugDraw : IDebugDraw {

    private val LOGGER = LogManager.getLogger(BulletDebugDraw::class)

    override var debugMode = if (Build.isDebug) (-1).and(NO_DEACTIVATION.inv()) else 0

    override fun reportErrorWarning(warningString: String) {
        LOGGER.warn(warningString)
    }

    override fun draw3dText(location: Vector3d, textString: String) {
        // is not being used by discrete dynamics world
    }

    private val tmpB = Vector3d()

    override fun drawContactPoint(
        position: Vector3d,
        normal: Vector3d,
        distance: Double, // pos = not touching, 0 = touching, neg = intersecting
        lifeTime: Int,
        color: Vector3d
    ) {
        // instead of a line, draw a shape with arrow
        val p1 = normal.add(position, tmpB)
        LineShapes.drawArrowZ(position, p1)
    }

    override fun drawLine(from: Vector3d, to: Vector3d, color: Vector3d) {
        LineBuffer.addLine(from, to, rgb(color.x.toInt(), color.y.toInt(), color.z.toInt()))
    }
}