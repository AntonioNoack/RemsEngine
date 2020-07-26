package me.anno.objects.effects

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.style.Style
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*

abstract class MaskedLayer(parent: Transform? = null): GFXTransform(parent){

    // just a little expensive...
    // todo enable multisampling
    val mask = Framebuffer(1, 1, 1, 1, false, Framebuffer.DepthBufferType.NONE)
    val masked = Framebuffer(1, 1, 1, 1, true, Framebuffer.DepthBufferType.INTERNAL)

    // limit to [0,1]?
    // nice effects can be created with values outside of [0,1], so while [0,1] is the valid range,
    // numbers outside [0,1] give artists more control
    var useMaskColor = AnimatedProperty.float()

    // not animated, because it's not meant to be transitioned, but instead to be a little helper
    var isInverted = false

    // ignore the bounds of this objects xy-plane?
    var isFullscreen = false

    // for user-debugging
    var showMask = false
    var showMasked = false
    var showFrame = true

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val showResult = GFX.isFinalRendering || (!showMask && !showMasked)
        if(children.size >= 2 && showResult){// else invisible

            /* (low priority)
            // to do calculate the size on screen to limit overhead
            // to do this additionally requires us to recalculate the transform
            if(!isFullscreen){
                val screenSize = GFX.windowSize
                val screenPositions = listOf(
                    Vector4f(-1f, -1f, 0f, 1f),
                    Vector4f(+1f, -1f, 0f, 1f),
                    Vector4f(-1f, +1f, 0f, 1f),
                    Vector4f(+1f, +1f, 0f, 1f)
                ).map {
                    stack.transformProject(it)
                }
            }*/

            BlendMode.DEFAULT.apply()

            drawMask(stack, time, color)

            BlendMode.DEFAULT.apply()

            drawMasked(stack, time, color)

            val effectiveBlendMode = getParentBlendMode(BlendMode.DEFAULT)
            effectiveBlendMode.apply()

            drawOnScreen(stack, time, color)

        }

        if(showMask) drawChild(stack, time, color, children.getOrNull(0))
        if(showMasked) drawChild(stack, time, color, children.getOrNull(1))

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // forced, because the default value might be true instead of false
        writer.writeBool("showMask", showMask, true)
        writer.writeBool("showMasked", showMasked, true)
        writer.writeBool("showFrame", showFrame, true)
        writer.writeBool("isFullscreen", isFullscreen, true)
        writer.writeBool("isInverted", isInverted, true)
        writer.writeObject(this, "useMaskColor", useMaskColor)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "showMask" -> showMask = value
            "showMasked" -> showMasked = value
            "showFrame" -> showFrame = value
            "isFullscreen" -> isFullscreen = value
            "isInverted" -> isInverted = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "useMaskColor" -> useMaskColor.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Invert Mask", "Changes transparency with opacity", null, isInverted, style){ isInverted = it }
        list += VI("Use Mask Color", "Should the color influence the masked?", useMaskColor, style)
        // todo expand plane to infinity if fullscreen -> depth works then, idk...
        // infinite bounds doesn't mean that it's actually filling the whole screen
        // (infinite horizon isn't covering both roof and floor)
        list += VI("Fullscreen", "if not, the borders are clipped by the quad shape", null, isFullscreen, style){ isFullscreen = it }
        list += SpacePanel(0, 1, style)
            .setColor(style.getChild("deep").getColor("background", black))
        list += VI("Show Mask", "for debugging purposes; shows the stencil", null, showMask, style){ showMask = it }
        list += VI("Show Masked", "for debugging purposes", null, showMasked, style){ showMasked = it }
        list += VI("Show Frame", "Only works correctly without camera depth", null, showFrame, style){ showFrame = it }
    }

    override fun drawChildrenAutomatically() = false

    fun drawMask(stack: Matrix4fArrayList, time: Double, color: Vector4f){

        mask.bindTemporary(GFX.windowWidth, GFX.windowHeight)

        val child = children.getOrNull(0)
        if(child?.getClassName() == "Transform" && child.children.isEmpty()){

            glClearColor(1f, 1f, 1f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)

        } else {

            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            drawChild(stack, time, color, child)

        }

    }

    fun drawMasked(stack: Matrix4fArrayList, time: Double, color: Vector4f){

        masked.bind(GFX.windowWidth, GFX.windowHeight)

        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        drawChild(stack, time, color, children.getOrNull(1))

        masked.unbind()

    }

    fun drawOnScreen(stack: Matrix4fArrayList, time: Double, color: Vector4f){

        val localTransform = if(isFullscreen){
            Matrix4fArrayList()
        } else {
            stack
        }

        val offsetColor = if(showFrame && !isFinalRendering) frameColor else invisible

        drawOnScreen(localTransform, time, color, offsetColor)

    }

    abstract fun drawOnScreen(localTransform: Matrix4fArrayList, time: Double, color: Vector4f, offsetColor: Vector4f)

    companion object {
        val frameColor = Vector4f(0.1f, 0.1f, 0.1f, 0.1f)
        val invisible = Vector4f(0f, 0f, 0f, 0f)
    }


}