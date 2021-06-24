package me.anno.ui.editor.sceneView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.deepDark
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderPlus
import me.anno.objects.Camera
import me.anno.studio.rems.Scene
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root
import me.anno.studio.rems.RemsStudio.targetHeight
import me.anno.studio.rems.RemsStudio.targetWidth
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.Maths.clamp01
import me.anno.utils.Maths.length
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.mixAngle
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f
import java.util.*
import kotlin.math.atan2

class ScenePreview(style: Style) : PanelList(null, style.getChild("sceneView")), ISceneView {

    init {

        weight = 1f
        backgroundColor = 0

    }

    val camera = nullCamera ?: Camera()

    // never the same :D
    var visualStateCtr = 0
    override fun getVisualState() = visualStateCtr++

    override val usesFPBuffers: Boolean get() = false
    override val isLocked2D get() = (System.currentTimeMillis() % 30000) > 25000

    // switch between manual control and autopilot for time :)
    // -> do this by disabling controls when playing, excepts when it's the inspector camera (?)
    var lastW = 0
    var lastH = 0
    var lastSizeUpdate = gameTime
    var goodW = 0
    var goodH = 0

    val random = Random()

    var target = Vector3f()
    var pos = Vector3f()

    private val movementSpeed = if(isLocked2D) 1f else 0.1f
    private val rotationSpeed get() = if(isLocked2D) 1f else 0.1f

    fun updatePosition() {
        val radius = 1.5f
        var distance: Float
        while (true) {
            distance = target.distance(pos)
            if (distance > 0.3f) break
            // find a new target
            target.x = random.nextGaussian().toFloat() * radius
            target.y = random.nextGaussian().toFloat() * radius
            target.z = random.nextGaussian().toFloat() + 3f
        }
        // go towards that target
        val deltaTime = GFX.rawDeltaTime
        val relativeMovement = clamp01(movementSpeed * deltaTime / (distance + 0.2f))
        val diff = pos - (target * 0.5f)
        val r0 = camera.rotationYXZ[0.0]
        val x0 = r0.x()
        val x1 = if(isLocked2D) 0f else 0.3f * atan2(diff.y, length(diff.x, diff.z)).toDegrees()
        val y0 = r0.y()
        val y1 = if(isLocked2D) 0f else atan2(diff.x, diff.z).toDegrees()
        val rs = clamp01(rotationSpeed * deltaTime)
        camera.rotationYXZ.set(Vector3f(mixAngle(x0, x1, rs), mixAngle(y0, y1, rs), 0f))
        pos.lerp(target, relativeMovement)
        camera.position.set(pos)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        GFX.check()

        updatePosition()

        drawRect(x, y, w, h, deepDark)

        var dx = 0
        var dy = 0
        var rw = w
        var rh = h

        val camera = camera
        if (camera.onlyShowTarget) {
            if (w * targetHeight > targetWidth * h) {
                rw = h * targetWidth / targetHeight
                dx = (w - rw) / 2
            } else {
                rh = w * targetHeight / targetWidth
                dy = (h - rh) / 2
            }
        }

        // check if the size stayed the same;
        // because resizing all framebuffers is expensive (causes lag)
        val matchesSize = lastW == rw && lastH == rh
        val wasNotRecentlyUpdated = lastSizeUpdate + 1e8 < gameTime
        if (matchesSize) {
            if (wasNotRecentlyUpdated) {
                goodW = rw
                goodH = rh
            }
        } else {
            lastSizeUpdate = gameTime
            lastW = rw
            lastH = rh
        }

        if (goodW == 0 || goodH == 0) {
            goodW = rw
            goodH = rh
        }

        drawRect(x + dx, y + dy, rw, rh, black)
        Scene.draw(
            camera, root,
            x + dx, y + dy,
            goodW, goodH,
            editorTime, false,
            Renderer.colorRenderer,
            this
        )

    }

}