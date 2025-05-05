package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType

object MeshAttributes {

    val coordsType = Attribute("positions", 3)
    val uvsType = Attribute("uvs", 2)
    val normalsType = Attribute("normals", AttributeType.UINT8_NORM, 3)
    val tangentsType = Attribute("tangents", AttributeType.UINT8_NORM, 4)
    val colorType = Attribute("color0", AttributeType.UINT8_NORM, 4)
    val boneIndicesType = Attribute("boneIndices", AttributeType.UINT8, 4)
    val boneWeightsType = Attribute("boneWeights", AttributeType.UINT8_NORM, 4)

    // colors, rgba,
    // the default shader only supports the first color
    // other colors still can be loaded for ... idk... maybe terrain information or sth like that
    var Mesh.color0: IntArray?
        get() = getAttr("color0", IntArray::class)
        set(value) = setAttr("color0", value, colorType)

    var Mesh.color1: IntArray?
        get() = getAttr("color1", IntArray::class)
        set(value) = setAttr("color1", value, colorType)

    var Mesh.color2: IntArray?
        get() = getAttr("color2", IntArray::class)
        set(value) = setAttr("color2", value, colorType)

    var Mesh.color3: IntArray?
        get() = getAttr("color3", IntArray::class)
        set(value) = setAttr("color3", value, colorType)

}