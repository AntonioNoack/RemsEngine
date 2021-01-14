package me.anno.objects.effects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFXx3D
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.studio.rems.Scene.mayUseMSAA
import me.anno.ui.base.Panel
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*

class MaskLayer(parent: Transform? = null) : GFXTransform(parent) {

    // just a little expensive...
    // todo why is multisampling sometimes black?
    // it's not yet production ready...
    val samples get() = if (useExperimentalMSAA && mayUseMSAA) 8 else 1

    lateinit var mask: Framebuffer
    lateinit var masked: Framebuffer

    var useExperimentalMSAA = false

    // limit to [0,1]?
    // nice effects can be created with values outside of [0,1], so while [0,1] is the valid range,
    // numbers outside [0,1] give artists more control
    val useMaskColor = AnimatedProperty.float()
    val blurThreshold = AnimatedProperty.float()
    val effectOffset = AnimatedProperty.pos2D()

    val greenScreenSimilarity = AnimatedProperty.float01(0.03f)
    val greenScreenSmoothness = AnimatedProperty.float01(0.01f)
    val greenScreenSpillValue = AnimatedProperty.float01(0.15f)
    fun greenScreenSettings(time: Double) =
        Vector3f(greenScreenSimilarity[time], greenScreenSmoothness[time], greenScreenSpillValue[time])

    // not animated, because it's not meant to be transitioned, but instead to be a little helper
    var isInverted = false

    // ignore the bounds of this objects xy-plane?
    var isFullscreen = false

    // for user-debugging
    var showMask = false
    var showMasked = false

    var type = MaskType.MASKING
    val effectSize = AnimatedProperty.float01exp(0.01f)

    override fun getSymbol() = DefaultConfig["ui.symbol.mask", "\uD83D\uDCA5"]

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val showResult = isFinalRendering || (!showMask && !showMasked)
        var needsDefault = false
        if (children.size >= 2 && showResult) {// else invisible

            val size = effectSize[time]

            if (size > 0f) {

                val w = GFX.windowWidth
                val h = GFX.windowHeight
                mask = FBStack["mask", w, h, samples, true]
                masked = FBStack["masked", w, h, samples, true]

                BlendDepth(null, false) {

                    // (low priority)
                    // to do calculate the size on screen to limit overhead
                    // to do this additionally requires us to recalculate the transform

                    BlendMode.DEFAULT.apply()

                    drawMask(stack, time, color)

                    BlendMode.DEFAULT.apply()

                    drawMasked(stack, time, color)
                }

                drawOnScreen(stack, time, color)

            } else {

                // draw default
                needsDefault = true

            }

        } else super.onDraw(stack, time, color)

        if (showMask && !isFinalRendering) drawChild(stack, time, color, children.getOrNull(0))
        if (needsDefault || (showMasked && !isFinalRendering)) drawChild(stack, time, color, children.getOrNull(1))

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // forced, because the default value might be true instead of false
        writer.writeBoolean("showMask", showMask, true)
        writer.writeBoolean("showMasked", showMasked, true)
        writer.writeBoolean("isFullscreen", isFullscreen, true)
        writer.writeBoolean("isInverted", isInverted, true)
        writer.writeBoolean("useMSAA", useExperimentalMSAA)
        writer.writeObject(this, "useMaskColor", useMaskColor)
        writer.writeObject(this, "blurThreshold", blurThreshold)
        writer.writeObject(this, "effectOffset", effectOffset)
        writer.writeInt("type", type.id)
        writer.writeObject(this, "pixelSize", effectSize)
        writer.writeObject(this, "greenScreenSimilarity", greenScreenSimilarity)
        writer.writeObject(this, "greenScreenSmoothness", greenScreenSmoothness)
        writer.writeObject(this, "greenScreenSpillValue", greenScreenSpillValue)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "showMask" -> showMask = value
            "showMasked" -> showMasked = value
            "isFullscreen" -> isFullscreen = value
            "isInverted" -> isInverted = value
            "useMSAA" -> useExperimentalMSAA = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "useMaskColor" -> useMaskColor.copyFrom(value)
            "blurThreshold" -> blurThreshold.copyFrom(value)
            "pixelSize" -> effectSize.copyFrom(value)
            "effectOffset" -> effectOffset.copyFrom(value)
            "greenScreenSimilarity" -> greenScreenSimilarity.copyFrom(value)
            "greenScreenSmoothness" -> greenScreenSmoothness.copyFrom(value)
            "greenScreenSpillValue" -> greenScreenSpillValue.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun drawChildrenAutomatically() = false

    fun drawMask(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        Frame(mask) {

            Frame.bind()

            val child = children.getOrNull(0)
            if (child?.getClassName() == "Transform" && child.children.isEmpty()) {

                glClearColor(1f, 1f, 1f, 1f)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            } else {

                glClearColor(0f, 0f, 0f, 0f)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                val oldDrawMode = GFX.drawMode
                if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

                drawChild(stack, time, color, child)

                GFX.drawMode = oldDrawMode

            }
        }

    }

    fun drawMasked(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        Frame(masked) {

            Frame.bind()

            val oldDrawMode = GFX.drawMode
            if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            drawChild(stack, time, color, children.getOrNull(1))

            GFX.drawMode = oldDrawMode

        }

    }

    // mask = 0, tex = 1
    fun drawOnScreen(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        GFX.check()

        val pixelSize = effectSize[time]
        val isInverted = if (isInverted) 1f else 0f

        val w = GFX.windowWidth
        val h = GFX.windowHeight

        val offset0 = effectOffset[time]
        val offset = Vector2f(offset0.x, offset0.y)

        when (type) {
            MaskType.GAUSSIAN_BLUR, MaskType.BLOOM -> {

                // done first blur everything, then mask
                // the artist could notice the fps going down, and act on his own (screenshot, rendering once, ...) ;)

                val oldDrawMode = GFX.drawMode
                if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

                val threshold = blurThreshold[time]
                GaussianBlur.draw(masked, pixelSize, w, h, 2, threshold, isFullscreen, stack)

                GFX.drawMode = oldDrawMode
                masked.bindTexture0(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                mask.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                GFX.check()

                GFXx3D.draw3DMasked(
                    stack, color,
                    type, useMaskColor[time],
                    pixelSize, offset, isInverted,
                    isFullscreen,
                    greenScreenSettings(time)
                )

            }
            MaskType.BOKEH_BLUR -> {

                val temp = FBStack["mask-bokeh", w, h, 1, true]

                val src0 = masked
                src0.bindTexture0(0, src0.textures[0].filtering, src0.textures[0].clamping)
                val srcBuffer = src0.msBuffer ?: src0
                BokehBlur.draw(srcBuffer.textures[0], temp, pixelSize)

                temp.bindTexture0(2, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                masked.bindTexture0(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                mask.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                GFXx3D.draw3DMasked(
                    stack, color,
                    type, useMaskColor[time],
                    0f, offset, isInverted,
                    isFullscreen,
                    greenScreenSettings(time)
                )

            }
            // awkward brightness bug; not production-ready
            /*MaskType.PIXELATING -> {

                val ih = clamp((2f / pixelSize).toInt(), 4, h)
                val iw = clamp(w * ih / h, 4, w)

                BoxBlur.draw(masked, w, h, iw, ih, 2, stack)

                masked.bindTexture0(1, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                mask.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)

                GFX.check()

                GFXx3D.draw3DMasked(
                    stack, color,
                    type, useMaskColor[time],
                    pixelSize, isInverted,
                    isFullscreen,
                    greenScreenSettings(time)
                )

            }*/
            else -> {

                masked.bindTextures(1, GPUFiltering.TRULY_NEAREST, Clamping.MIRRORED_REPEAT)
                GFX.check()
                mask.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                GFX.check()
                GFXx3D.draw3DMasked(
                    stack, color,
                    type, useMaskColor[time],
                    pixelSize, offset, isInverted,
                    isFullscreen,
                    greenScreenSettings(time)
                )

            }
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val mask = getGroup("Mask Settings", "Masks are multipurpose objects", "mask")
        mask += vi("Type", "Specifies what kind of mask it is", null, type, style) { type = it }

        fun typeSpecific(panel: Panel, isVisible: (MaskType) -> Boolean) {
            mask += panel
            mask += SpyPanel(style) { panel.visibility = Visibility[isVisible(type)] }
        }

        mask += vi("Size", "How large pixelated pixels or blur should be", effectSize, style)
        mask += vi("Invert Mask", "Changes transparency with opacity", null, isInverted, style) { isInverted = it }
        mask += vi("Use Color / Transparency", "Should the color influence the masked?", useMaskColor, style)
        typeSpecific(vi("Effect Center", "", effectOffset, style)) {
            when (it) {
                MaskType.RADIAL_BLUR_1, MaskType.RADIAL_BLUR_2 -> true
                else -> false
            }
        }
        typeSpecific(vi("Blur Threshold", "", blurThreshold, style)) {
            when (it) {
                MaskType.GAUSSIAN_BLUR, MaskType.BLOOM -> true
                else -> false
            }
        }
        mask += vi(
            "Make Huge", "Scales the mask, without affecting the children", null,
            isFullscreen, style
        ) { isFullscreen = it }
        mask += vi(
            "Use MSAA(!)",
            "MSAA is experimental, may not always work",
            null,
            useExperimentalMSAA,
            style
        ) { useExperimentalMSAA = it }
        val greenScreen =
            getGroup("Green Screen", "Type needs to be green-screen; cuts out a specific color", "greenScreen")
        list += SpyPanel(style) { greenScreen.visibility = Visibility[type == MaskType.GREEN_SCREEN] }
        greenScreen += vi("Similarity", "", greenScreenSimilarity, style)
        greenScreen += vi("Smoothness", "", greenScreenSmoothness, style)
        greenScreen += vi("Spill Value", "", greenScreenSpillValue, style)
        val editor = getGroup("Editor", "", "editor")
        editor += vi("Show Mask", "for debugging purposes; shows the stencil", null, showMask, style) { showMask = it }
        editor += vi("Show Masked", "for debugging purposes", null, showMasked, style) { showMasked = it }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "type" -> type = MaskType.values().firstOrNull { it.id == value } ?: type
            else -> super.readInt(name, value)
        }
    }

    override fun getDefaultDisplayName() = "Mask Layer"
    override fun getClassName() = "MaskLayer"

    companion object {
        fun create(mask: List<Transform>?, masked: List<Transform>?): MaskLayer {
            val maskLayer = MaskLayer(null)
            val mask2 = Transform(maskLayer)
            mask2.name = "Mask Folder"
            if (mask == null) {
                Circle(mask2).innerRadius.set(0.5f)
            } else mask.forEach { mask2.addChild(it) }
            val masked2 = Transform(maskLayer)
            masked2.name = "Masked Folder"
            if (masked == null) {
                Polygon(masked2)
            } else masked.forEach { masked2.addChild(it) }
            return maskLayer
        }
    }

}