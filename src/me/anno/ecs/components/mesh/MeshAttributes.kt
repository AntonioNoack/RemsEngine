package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType

/**
 * Typically used attributes by Mesh-based algorithms.
 * */
object MeshAttributes {

    val coordsType = Attribute("positions", 3)
    val uvsType = Attribute("uvs", 2)
    val normalsType = Attribute("normals", AttributeType.SINT8_NORM, 3)
    val tangentsType = Attribute("tangents", AttributeType.SINT8_NORM, 4)
    val boneIndicesType = Attribute("boneIndices", AttributeType.UINT8, 4)
    val boneWeightsType = Attribute("boneWeights", AttributeType.UINT8_NORM, 4)

    private val color0Type = Attribute("colors0", AttributeType.UINT8_NORM, 4)
    private val color1Type = Attribute("colors1", AttributeType.UINT8_NORM, 4)
    private val color2Type = Attribute("colors2", AttributeType.UINT8_NORM, 4)
    private val color3Type = Attribute("colors3", AttributeType.UINT8_NORM, 4)

    // colors, rgba,
    // the default shader only supports the first color
    // other colors still can be loaded for ... idk... maybe terrain information or sth like that
    var Mesh.color0: IntArray?
        get() = getAttr("colors0", IntArray::class)
        set(value) = setAttr("colors0", value, color0Type)

    var Mesh.color1: IntArray?
        get() = getAttr("colors1", IntArray::class)
        set(value) = setAttr("colors1", value, color1Type)

    var Mesh.color2: IntArray?
        get() = getAttr("colors2", IntArray::class)
        set(value) = setAttr("colors2", value, color2Type)

    var Mesh.color3: IntArray?
        get() = getAttr("colors3", IntArray::class)
        set(value) = setAttr("colors3", value, color3Type)
}