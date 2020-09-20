package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*

class MaskLayer(parent: Transform? = null) : MaskLayerBase(parent) {

    var type = MaskType.MASKING
    val effectSize = AnimatedProperty.float01exp(0.01f)

    // mask = 0, tex = 1
    override fun drawOnScreen(localTransform: Matrix4fArrayList, time: Double, color: Vector4f, offsetColor: Vector4f) {
        val pixelSize = effectSize[time]
        val isInverted = if (isInverted) 1f else 0f
        when (type) {
            MaskType.GAUSSIAN_BLUR -> {

                // todo sample down for large blur sizes?? (for performance reasons)
                // done first blur everything, then mask
                // the artist could notice the fps going down, and act on his own (screenshot, rendering once, ...) ;)

                GFX.check()
                masked.bindTexture0(0, true, ClampMode.CLAMP)
                GFX.check()

                fun drawBlur(fb: Framebuffer, offset: Int, isFirst: Boolean) {
                    // step1
                    fb.bind(GFX.windowWidth, GFX.windowHeight)
                    // clear to black?
                    glClear(GL_DEPTH_BUFFER_BIT)
                    GFX.draw3DBlur(
                        localTransform, pixelSize,
                        if (isFirst)
                            Vector2f(0f, pixelSize)
                        else
                            Vector2f(pixelSize * GFX.windowHeight / GFX.windowWidth, 0f)
                    )
                    fb.unbind()
                    fb.bindTexture0(offset, true, ClampMode.CLAMP)
                }

                val oldDrawMode = GFX.drawMode
                if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

                val bd = BlendDepth(null, false)
                bd.bind()

                drawBlur(temp, 0, true)
                drawBlur(temp2, 2, false)

                bd.unbind()

                GFX.drawMode = oldDrawMode

                masked.bindTexture0(1, true, ClampMode.CLAMP)
                mask.bindTexture0(0, true, ClampMode.CLAMP)
                GFX.check()

                GFX.draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time], offsetColor,
                    pixelSize, isInverted
                )
            }
            MaskType.BOKEH_BLUR -> {
                // todo not working correctly
                // calculate and apply bokeh...
                temp.bind(GFX.windowWidth, GFX.windowHeight)
                // masked.bindTexture0(0, true)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

                val bd = BlendDepth(null, false)
                bd.bind()

                val src0 = masked
                val srcBuffer = src0.msBuffer ?: src0
                BokehBlur.draw(srcBuffer.textures[0], pixelSize)
                temp.unbind()
                temp.bindTexture0(1, true, ClampMode.CLAMP)

                bd.unbind()

                GFX.draw3DMasked(
                    localTransform, color,
                    MaskType.GAUSSIAN_BLUR, useMaskColor[time], offsetColor,
                    0f, isInverted
                )

            }
            else -> {
                GFX.check()
                masked.bindTextures(1, true, ClampMode.MIRRORED_REPEAT)
                GFX.check()
                mask.bindTextures(0, true, ClampMode.CLAMP)
                GFX.check()
                GFX.draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time], offsetColor,
                    pixelSize, isInverted
                )
            }
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Type", "Masks are multipurpose objects", null, type, style) { type = it }
        list += VI(
            "Effect Size",
            "How large pixelated pixels or blur should be",
            effectSize,
            style
        )
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