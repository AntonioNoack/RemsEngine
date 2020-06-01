package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*

class MaskLayer(parent: Transform?): GFXTransform(parent){

    // just a little expensive...
    val mask = Framebuffer(1, 1, 1, false, false)
    val masked = Framebuffer(1, 1, 1, true, true)

    var useMaskColor = AnimatedProperty.float()

    var showMask = false
    var showMasked = false

    var showFrame = true

    var isFullscreen = false

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f) {

        BlendMode.DEFAULT.apply()

        drawMask(stack, time, color)

        BlendMode.DEFAULT.apply()

        drawMasked(stack, time, color)

        val effectiveBlendMode = getParentBlendMode(BlendMode.DEFAULT)
        effectiveBlendMode.apply()

        drawOnScreen(stack, time, color)

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
        writer.writeObject(this, "useMaskColor", useMaskColor)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "showMask" -> showMask = value
            "showMasked" -> showMasked = value
            "showFrame" -> showFrame = value
            "isFullscreen" -> isFullscreen = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "useMaskColor" -> if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.FLOAT){
                useMaskColor = value as AnimatedProperty<Float>
            }
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += BooleanInput("Show Mask", showMask, style)
            .setChangeListener { showMask = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Show Masked", showMasked, style)
            .setChangeListener { showMasked = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Show Frame", showFrame, style)
            .setChangeListener { showFrame = it }
            .setIsSelectedListener { show(null) }
        list += FloatInput("Use Mask Color", useMaskColor, lastLocalTime, style)
            .setChangeListener { putValue(useMaskColor, it) }
            .setIsSelectedListener { show(useMaskColor) }
        list += BooleanInput("Fullscreen", isFullscreen, style)
            .setChangeListener { isFullscreen = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("Only works correctly without camera depth") // todo make it so the plane is used as depth
    }

    override fun drawChildrenAutomatically() = false

    fun drawMask(stack: Matrix4fStack, time: Float, color: Vector4f){

        mask.bindTemporary(GFX.windowWidth, GFX.windowHeight)

        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val child = children.getOrNull(0)
        if(child != null) drawChild(stack, time, color, child)

    }

    fun drawMasked(stack: Matrix4fStack, time: Float, color: Vector4f){

        masked.bind(GFX.windowWidth, GFX.windowHeight)

        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val child = children.getOrNull(1)
        if(child != null) drawChild(stack, time, color, child)

        masked.unbind()

    }

    fun drawOnScreen(stack: Matrix4fStack, time: Float, color: Vector4f){

        val localTransform = if(isFullscreen){
            Matrix4f()
        } else {
            stack
        }

        // todo don't show offset while rendering
        val offsetColor = if(showFrame) frameColor else invisible
        GFX.draw3DMasked(localTransform, masked.textures[0], mask.textures[0], color,
            isBillboard[time], true, useMaskColor[time], offsetColor)

    }

    override fun getClassName(): String = "MaskLayer"

    companion object {
        val frameColor = Vector4f(0.1f, 0.1f, 0.1f, 0.1f)
        val invisible = Vector4f(0f, 0f, 0f, 0f)
    }


}