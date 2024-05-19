package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData
import org.joml.Vector3f

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_armature_types.h
 * */
@Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
class BBone(ptr: ConstructorData) : BLink<BBone>(ptr) {

    val name = string("name[64]", 64)
    val parent = getPointer("*parent") as? BBone

    val children = inside("childbase") as BListBase<BBone>

    val roll = float("roll")
    val head = floats("head[3]", 3)
    val tail = floats("tail[3]", 3)
    val boneMat = floats("bone_mat[3][3]", 9)

    val restPose = floats("arm_mat[4][4]", 16)

    /*
    *  *next: Bone, *prev: Bone, *prop: IDProperty, *parent: Bone, childbase: ListBase,
    * name[64]: char, roll: float, head[3]: float, tail[3]: float, bone_mat[3][3]: float, flag: int,
    * inherit_scale_mode: char,
    *
    * arm_head[3]: float, arm_tail[3]: float, arm_mat[4][4]: float, arm_roll: float,
    * dist: float, weight: float, xwidth: float, length: float, zwidth: float, rad_head: float, rad_tail: float,
    * roll1: float, roll2: float, curveInX: float, curveInY: float, curveOutX: float, curveOutY: float,
    * ease1: float, ease2: float,
    * scaleIn: float, scale_in_y: float, scaleOut: float, scale_out_y: float, scale_in[3]: float, scale_out[3]: float,
    * size[3]: float, layer: int, segments: short, bbone_prev_type: char, bbone_next_type: char,
    * bbone_flag: int, bbone_prev_flag: short, bbone_next_flag: short, *bbone_prev: Bone, *bbone_next: Bone }
    * */

    override fun toString(): String {
        return "Bone { '$name', parent: ${parent?.name}, head: ${Vector3f(head)}, tail: ${Vector3f(tail)}, children: ${children.size}x }"
    }

}