package me.anno.bullet

import com.bulletphysics.linearmath.IDebugDraw
import me.anno.engine.ui.LineShapes
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.buffer.LineBuffer.vToByte
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import javax.vecmath.Vector3d

object BulletDebugDraw : IDebugDraw {

    private val LOGGER = LogManager.getLogger(BulletDebugDraw::class)

    val stack = Matrix4f()
    val cam = org.joml.Vector3d()

    /**
    public static final int NO_DEBUG              = 0;
    public static final int DRAW_WIREFRAME        = 1;
    public static final int DRAW_AABB             = 2;
    public static final int DRAW_FEATURES_TEXT    = 4;
    public static final int DRAW_CONTACT_POINTS   = 8;
    public static final int NO_DEACTIVATION       = 16;
    public static final int NO_HELP_TEXT          = 32;
    public static final int DRAW_TEXT             = 64;
    public static final int PROFILE_TIMINGS       = 128;
    public static final int ENABLE_SAT_COMPARISON = 256;
    public static final int DISABLE_BULLET_LCP    = 512;
    public static final int ENABLE_CCD            = 1024;
    public static final int MAX_DEBUG_DRAW_MODE   = 1025;
     * */

    override var debugMode = 2047 // all flags

    override fun reportErrorWarning(warningString: String) {
        LOGGER.warn(warningString)
    }

    override fun draw3dText(location: Vector3d, textString: String) {
        // is not being used by discrete dynamics world
    }

    private val tmpA = org.joml.Vector3d()
    private val tmpB = org.joml.Vector3d()

    override fun drawContactPoint(
        position: Vector3d,
        normal: Vector3d,
        distance: Double, // pos = not touching, 0 = touching, neg = intersecting
        lifeTime: Int,
        color: Vector3d
    ) {
        // instead of a line, draw a shape with arrow
        val p0 = tmpA.set(position.x, position.y, position.z)
        val p1 = tmpB.set(normal.x, normal.y, normal.z).add(p0)
        LineShapes.drawArrowZ(p0, p1)
    }

    override fun drawLine(from: Vector3d, to: Vector3d, color: Vector3d) {
        LineBuffer.putRelativeLine(
            from.x, from.y, from.z, to.x, to.y, to.z, cam,
            vToByte(color.x), vToByte(color.y), vToByte(color.z), -1
        )
    }
}