package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_armature_types.h
 * */
@Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
class BArmature(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    // id: ID, *adt: AnimData, bonebase: ListBase,
    // *bonehash: GHash, // look up bones by name
    //
    // *edbo: ListBase, *act_bone: Bone, *act_edbone: EditBone,
    // needs_flush_to_id: char,
    //
    // flag: int, drawtype: int, deformflag: short, pathflag: short,
    // layer_used: int, layer: int, layer_protected: int, axes_position: float

    val id = inside("id") as BID
    val bones = inside("bonebase") as BListBase<BBone>
    val adt = getPointer("*adt")

    override fun toString(): String {
        return "Armature { $id, $adt, $bones }"
    }
}