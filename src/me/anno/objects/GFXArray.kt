package me.anno.objects

import me.anno.io.text.TextReader
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.joml.Vector4f

class GFXArray(parent: Transform?): GFXTransform(parent) {

    var perChildLocation = AnimatedProperty.pos()
    var perChildRotation = AnimatedProperty.rotYXZ()
    var perChildScale = AnimatedProperty.scale()
    // per child skew?

    var instanceCount = 10

    fun updateChildren(){
        children.removeAll(children.filterIndexed { index, transform -> index >= instanceCount })
        if(children.size in 0 until instanceCount){
            val base = children.first().stringify()
            while(children.size < instanceCount){
                addChild(object: Transform(null){
                    init {
                        val clone = TextReader.fromText(base).first { it is Transform } as Transform
                        children.add(clone)

                    }
                })
            }
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        // todo create apply button?
        // todo we need to be able to insert properties...
        // todo replace? :D, # String Array
    }


}