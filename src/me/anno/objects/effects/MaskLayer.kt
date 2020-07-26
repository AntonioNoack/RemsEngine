package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

class MaskLayer(parent: Transform? = null): MaskedLayer(parent){

    var type = MaskType.MASKING
    val pixelSize = AnimatedProperty.float01exp().set(0.01f)

    override fun drawOnScreen(localTransform: Matrix4fArrayList, time: Double, color: Vector4f, offsetColor: Vector4f) {
        GFX.draw3DMasked(localTransform, masked.textures[0], mask.textures[0], color,
            isBillboard[time], true, type, useMaskColor[time], offsetColor,
            pixelSize[time],
            if(isInverted) 1f else 0f)
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Type", "", null, type, style){ type = it }
        list += VI("Pixel Size", "How large pixelated pixels should be, type = ${MaskType.PIXELATING.displayName}", pixelSize, style)
    }

    override fun getDefaultDisplayName() = "Mask Layer"
    override fun getClassName() = "MaskLayer"

}