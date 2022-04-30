package me.anno.ecs.components.mesh

import me.anno.io.base.BaseWriter

// todo decal pass:
//  Input: pos, normal (we could pass in color theoretically, but idk)
//  Output: new pos, new normal, new emissive
// todo different blend modes: additive, subtractive, default, ...
class DecalMaterial : Material() {

    // can we support this in forward rendering?
    // yes, but it will be a bit more expensive

    var writeColor = true
    var writeNormal = false
    var writeDepth = false
    var writeEmissive = false
    var writeRoughness = false
    var writeMetallic = false

    // theoretically, this also could support applying clear-coat

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("writeColor", writeColor)
        writer.writeBoolean("writeNormal", writeNormal)
        writer.writeBoolean("writeDepth", writeDepth)
        writer.writeBoolean("writeEmissive", writeEmissive)
        writer.writeBoolean("writeRoughness", writeRoughness)
        writer.writeBoolean("writeMetallic", writeMetallic)
    }

    override val className = "DecalMaterial"

}