package me.anno.objects.effects

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
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
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*

class MaskLayer(parent: Transform? = null): GFXTransform(parent){

    // just a little expensive...
    val mask = Framebuffer(1, 1, 1, 1, false, Framebuffer.DepthBufferType.NONE)
    val masked = Framebuffer(1, 1, 1, 1, true, Framebuffer.DepthBufferType.INTERNAL)

    // limit to 01?
    var useMaskColor = AnimatedProperty.float()

    // not animated, because it's not meant to be transitioned, but instead to be a little helper
    var isInverted = false

    // ignore the bounds of this objects xy-plane?
    var isFullscreen = false

    // for user-debugging
    var showMask = false
    var showMasked = false
    var showFrame = true

    override fun onDraw(stack: Matrix4fArrayList, time: Float, color: Vector4f) {

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
        list += BooleanInput("Invert Mask", isInverted, style)
            .setChangeListener { isInverted = it }
            .setIsSelectedListener { show(null) }
        list += VI("Use Mask Color", "Should the color influence the masked?", useMaskColor, style)
        list += BooleanInput("Fullscreen", isFullscreen, style)
            .setChangeListener { isFullscreen = it }
            .setIsSelectedListener { show(null) }
        list += SpacePanel(0, 1, style)
            .setColor(style.getChild("deep").getColor("background", black))
        list += BooleanInput("Show Mask", showMask, style)
            .setChangeListener { showMask = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Show Masked", showMasked, style)
            .setChangeListener { showMasked = it }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Show Frame", showFrame, style)
            .setChangeListener { showFrame = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("Only works correctly without camera depth") // todo make it so the plane is used as depth
    }

    override fun drawChildrenAutomatically() = false

    fun drawMask(stack: Matrix4fArrayList, time: Float, color: Vector4f){

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

    fun drawMasked(stack: Matrix4fArrayList, time: Float, color: Vector4f){

        masked.bind(GFX.windowWidth, GFX.windowHeight)

        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        drawChild(stack, time, color, children.getOrNull(1))

        masked.unbind()

    }

    fun drawOnScreen(stack: Matrix4fArrayList, time: Float, color: Vector4f){

        val localTransform = if(isFullscreen){
            Matrix4fArrayList()
        } else {
            stack
        }

        // todo don't show offset while rendering
        val offsetColor = if(showFrame) frameColor else invisible

        GFX.draw3DMasked(localTransform, masked.textures[0], mask.textures[0], color,
            isBillboard[time], true, useMaskColor[time], offsetColor,
            if(isInverted) 1f else 0f)

    }

    override fun getDefaultDisplayName() = "Mask Layer"
    override fun getClassName(): String = "MaskLayer"

    companion object {
        val frameColor = Vector4f(0.1f, 0.1f, 0.1f, 0.1f)
        val invisible = Vector4f(0f, 0f, 0f, 0f)
    }


}