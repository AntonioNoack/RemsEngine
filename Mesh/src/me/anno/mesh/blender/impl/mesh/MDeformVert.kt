package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BInstantList
import me.anno.mesh.blender.impl.BlendData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_meshdata_types.h#L182
 * */
@Suppress("SpellCheckingInspection")
class MDeformVert(ptr: ConstructorData) : BlendData(ptr) {

    // { *dw: MDeformWeight, totweight: int, flag: int }
    val weights get() = getInstantList<MDeformWeight>("*dw", numWeights) ?: BInstantList.emptyList()
    val numWeights get() = int("totweight")

    override fun toString(): String {
        return "MDeformVert@$position($weights)"
    }

}