package me.anno.objects.effects

import me.anno.gpu.GFX
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

class MaskLayer(parent: Transform? = null): MaskedLayer(parent){

    var type = MaskType.MASKING
    val pixelSize = AnimatedProperty.float01exp(0.01f)

    // mask = 0, tex = 1
    override fun drawOnScreen(localTransform: Matrix4fArrayList, time: Double, color: Vector4f, offsetColor: Vector4f) {
        val pixelSize = pixelSize[time]
        when(type){
            MaskType.GAUSSIAN_BLUR -> {
                // todo sample down for large blur sizes?? (for performance reasons)
                // todo first blur everything, then mask?
                // the artist could notice the fps going down, and act on his own (screenshot, rendering once, ...) ;)
                GFX.check()
                masked.bindTexture0(1, true, ClampMode.CLAMP)
                GFX.check()
                mask.bindTexture0(0, true, ClampMode.CLAMP)
                GFX.check()
                temp.bind(GFX.windowWidth, GFX.windowHeight)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                glDisable(GL_BLEND)
                glDisable(GL_DEPTH_TEST)
                GFX.draw3DMasked(localTransform, color,
                    type, useMaskColor[time], offsetColor,
                    pixelSize, if(isInverted) 1f else 0f, Vector2f(0f, pixelSize)
                )
                glEnable(GL_BLEND)
                glEnable(GL_DEPTH_TEST) // todo only if camera wishes so
                temp.unbind()
                temp.bindTexture0(1, true, ClampMode.CLAMP) // becomes masked
                GFX.draw3DMasked(localTransform, color,
                    type, useMaskColor[time], offsetColor,
                    pixelSize, if(isInverted) 1f else 0f, Vector2f(pixelSize * GFX.windowHeight / GFX.windowWidth, 0f)
                )
            }
            MaskType.BOKEH_BLUR -> {
                // todo not working correctly
                // calculate and apply bokeh...
                temp.bind(GFX.windowWidth, GFX.windowHeight)
                // masked.bindTexture0(0, true)
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                glDisable(GL_BLEND)
                glDisable(GL_DEPTH_TEST)
                val src0 = masked
                val srcBuffer = src0.msBuffer ?: src0
                BokehBlur.draw(srcBuffer.textures[0], pixelSize)
                temp.unbind()
                temp.bindTexture0(1, true, ClampMode.CLAMP)
                glEnable(GL_BLEND)
                glEnable(GL_DEPTH_TEST) // todo only if camera wishes so
                GFX.draw3DMasked(localTransform, color,
                    MaskType.GAUSSIAN_BLUR, useMaskColor[time], offsetColor,
                    0f, if(isInverted) 1f else 0f, Vector2f(0f, 0f)
                )
            }
            else -> {
                GFX.check()
                masked.bindTextures(1, true, ClampMode.CLAMP)
                GFX.check()
                mask.bindTextures(0, true, ClampMode.CLAMP)
                GFX.check()
                GFX.draw3DMasked(localTransform, color,
                    type, useMaskColor[time], offsetColor,
                    pixelSize, if(isInverted) 1f else 0f, Vector2f(0f, 0f)
                )
            }
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Type", "Masks are multipurpose objects", null, type, style){ type = it }
        list += VI("Pixel Size", "How large pixelated pixels should be, type = ${MaskType.PIXELATING.displayName}", pixelSize, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("type", type.id)
        writer.writeObject(this, "pixelSize", pixelSize)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "type" -> type = MaskType.values().firstOrNull { it.id == value } ?: type
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "pixelSize" -> pixelSize.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun getDefaultDisplayName() = "Mask Layer"
    override fun getClassName() = "MaskLayer"

}