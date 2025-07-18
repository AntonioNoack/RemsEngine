package me.anno.bullet

import com.bulletphysics.linearmath.DebugDrawModes.NO_DEACTIVATION
import com.bulletphysics.linearmath.IDebugDraw
import me.anno.engine.ui.LineShapes
import me.anno.gpu.buffer.LineBuffer
import me.anno.utils.Color.rgb
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

object BulletDebugDraw : IDebugDraw {

    private val LOGGER = LogManager.getLogger(BulletDebugDraw::class)

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

    // all flags except deactivation
    override val debugMode = 2047.and(NO_DEACTIVATION.inv())

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