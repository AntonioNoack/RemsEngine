package me.anno.objects.effects

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
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