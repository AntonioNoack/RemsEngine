package me.anno.bullet

import com.bulletphysics.linearmath.IDebugDraw
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.buffer.LineBuffer
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import javax.vecmath.Vector3d

class BulletDebugDraw : IDebugDraw() {

    companion object {
        private val LOGGER = LogManager.getLogger(BulletDebugDraw::class)
    }

    var stack = Matrix4f()
    var cam = org.joml.Vector3d()
    val worldScale get() = RenderState.worldScale

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

    var mode = 2047 // all flags

    // private val textInstance = Text()

    override fun getDebugMode() = mode

    override fun setDebugMode(debugMode: Int) {
        mode = debugMode
    }

    override fun reportErrorWarning(warningString: String) {
        LOGGER.warn(warningString)
    }

    override fun draw3dText(location: Vector3d, textString: String) {
        // is not being used by discrete dynamics world
        /*val localPosition = toLocal(location)
        textInstance.position.set(localPosition)
        // set scale based on distance
        val distance = localPosition.length()
        val defaultScale = 1f
        textInstance.scale.set(Vector3f(defaultScale / distance))
        textInstance.alignWithCamera.set(1f)
        textInstance.text.set(textString)
        textInstance.draw(stack, 1.0, white4)*/
    }

    /*private fun toLocal(global: Vector3d): Vector3f {
        return Vector3f(
            ((global.x - cam.x) * worldScale).toFloat(),
            ((global.y - cam.y) * worldScale).toFloat(),
            ((global.z - cam.z) * worldScale).toFloat()
        )
    }

    private fun toColor(color: Vector3d, alpha: Float = 1f): Vector4f {
        return Vector4f(color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), alpha)
    }*/

    override fun drawContactPoint(
        position: Vector3d,
        normal: Vector3d,
        distance: Double, // pos = not touching, 0 = touching, neg = intersecting
        lifeTime: Int,
        color: Vector3d
    ) {
        // instead of a line, draw a shape with arrow
        val p0 = JomlPools.vec3d.create().set(position.x, position.y, position.z)
        val p1 = JomlPools.vec3d.create().set(normal.x, normal.y, normal.z).add(p0)
        LineShapes.drawArrowZ(p0, p1)
        // drawLine(position, Vector3d(position).apply { add(normal) }, color)
    }

    override fun drawLine(from: Vector3d, to: Vector3d, color: Vector3d) {
        LineBuffer.putRelativeLine(from, to, cam, worldScale, color.x, color.y, color.z)
        // Grid.drawLine(stack, toColor(color), toLocal(from), toLocal(to))
    }

}