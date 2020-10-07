package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowHeight
import me.anno.gpu.GFX.windowWidth
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
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import kotlin.math.max

class MaskLayer(parent: Transform? = null) : MaskLayerBase(parent) {

    var type = MaskType.MASKING
    val effectSize = AnimatedProperty.float01exp(0.01f)

    companion object {
        fun create(mask: List<Transform>?, masked: List<Transform>?): MaskLayer {
            val maskLayer = MaskLayer(null)
            val mask2 = Transform(maskLayer)
            mask2.name = "Mask Folder"
            if(mask == null){
                Circle(mask2).innerRadius.set(0.5f)
            } else mask.forEach { mask2.addChild(it) }
            val masked2 = Transform(maskLayer)
            masked2.name = "Masked Folder"
            if(masked == null){
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

                var size = pixelSize

                // done first blur everything, then mask
                // the artist could notice the fps going down, and act on his own (screenshot, rendering once, ...) ;)

                fun drawBlur(target: Framebuffer, w: Int, h: Int, offset: Int, isFirst: Boolean) {
                    // step1
                    Frame(w, h, true, target){
                        glClear(GL_DEPTH_BUFFER_BIT)
                        GFX.draw3DBlur(localTransform, size, w, h, isFirst)
                    }
                    target.bindTexture0(offset,
                        if(true || isFirst || size == pixelSize) NearestMode.NEAREST
                        else NearestMode.LINEAR, ClampMode.CLAMP)
                }

                val oldDrawMode = GFX.drawMode
                if (oldDrawMode == ShaderPlus.DrawMode.COLOR_SQUARED) GFX.drawMode = ShaderPlus.DrawMode.COLOR

                GFX.check()
                masked.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                GFX.check()

                BlendDepth(null, false).use {

                    val steps = pixelSize * windowHeight
                    val subSteps = (steps / 10f).toInt()

                    var smallerW = w
                    var smallerH = h

                    val debug = false

                    // sample down for large blur sizes for performance reasons
                    if(debug && subSteps > 1){
                        // smallerW /= 2
                        // smallerH /= 2
                        smallerW = max(10, w / subSteps)
                        if(debug && 'J'.toInt() in keysDown) smallerH = max(10, h / subSteps)
                        // smallerH /= 2
                        // smallerH = max(10, h / subSteps)
                        size = pixelSize * smallerW / w
                        // draw image on smaller thing...
                        val temp2 = FBStack["mask-gaussian-blur-2", smallerW, smallerH, 1, true]// temp[2]
                        Frame(smallerW, smallerH, false, temp2){
                            // glClearColor(0f, 0f, 0f, 0f)
                            // glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
                            // draw texture 0 (masked) onto temp2
                            // todo sample multiple times...
                            GFX.copy()
                            temp2.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                        }
                    }

                    if(debug && 'I'.toInt() in keysDown) println("$w,$h -> $smallerW,$smallerH")

                    drawBlur(FBStack["mask-gaussian-blur-0", smallerW, smallerH, 1, true], smallerW, smallerH, 0, true)
                    drawBlur(FBStack["mask-gaussian-blur-1", smallerW, smallerH, 1, true], smallerW, smallerH, 2, false)

                }

                GFX.drawMode = oldDrawMode

                masked.bindTexture0(1, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                mask.bindTexture0(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)

                GFX.check()

                GFX.draw3DMasked(
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

                GFX.draw3DMasked(
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
                GFX.draw3DMasked(
                    localTransform, color,
                    type, useMaskColor[time],
                    pixelSize, isInverted
                )
            }
        }
    }

    override fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, id: String) -> SettingCategory) {
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