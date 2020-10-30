package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowHeight
import me.anno.gpu.GFX.windowWidth
import me.anno.gpu.GFXx3D.draw3DBlur
import me.anno.gpu.GFXx3D.draw3DMasked
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.NearestMode
import me.anno.input.Input.keysDown
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import kotlin.math.max

class MaskLayer(parent: Transform? = null) : MaskLayerBase(parent) {

    var type = MaskType.MASKING
    val effectSize = AnimatedProperty.float01exp(0.01f)

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

    // mask = 0, tex = 1
    override fun drawOnScreen2(localTransform: Matrix4fArrayList, time: Double, color: Vector4f) {

        val pixelSize = effectSize[time]
        val isInverted = if (isInverted) 1f else 0f

        val w = windowWidth
        val h = windowHeight

        when (type) {
            MaskType.GAUSSIAN_BLUR -> {

                // done first blur everything, then mask
                // the artist could notice the fps going down, and act on his own (screenshot, rendering once, ...) ;)

                val oldDrawMode = GFX.drawMode
                if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

                GFX.check()

                GaussianBlur.draw(masked, pixelSize, w, h, 2, 0f, localTransform)

                GFX.drawMode = oldDrawMode
                masked.bindTexture0(1, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                mask.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)

                GFX.check()

                draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time],
                    pixelSize, isInverted
                )

            }
            MaskType.BOKEH_BLUR -> {

                val temp = FBStack["mask-bokeh", w, h, 1, true]

                // todo not working correctly for large sizes because of too small sample size
                // calculate and apply bokeh...

                val src0 = masked
                val srcBuffer = src0.msBuffer ?: src0
                BokehBlur.draw(srcBuffer.textures[0], temp, pixelSize)

                temp.bindTexture0(2, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                masked.bindTexture0(1, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                mask.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)

                draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time],
                    0f, isInverted
                )

            }
            else -> {
                GFX.check()
                masked.bindTextures(1, NearestMode.TRULY_NEAREST, ClampMode.MIRRORED_REPEAT)
                GFX.check()
                mask.bindTextures(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                GFX.check()
                draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time],
                    pixelSize, isInverted
                )
            }
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val effect = getGroup("Effect", "fx")
        effect += VI("Type", "Masks are multipurpose objects", null, type, style) { type = it }
        effect += VI("Size", "How large pixelated pixels or blur should be", effectSize, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("type", type.id)
        writer.writeObject(this, "pixelSize", effectSize)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "type" -> type = MaskType.values().firstOrNull { it.id == value } ?: type
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "pixelSize" -> effectSize.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun getDefaultDisplayName() = "Mask Layer"
    override fun getClassName() = "MaskLayer"

}