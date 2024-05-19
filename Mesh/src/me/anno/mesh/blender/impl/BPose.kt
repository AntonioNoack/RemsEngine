package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

// armature pose
/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L495
 * */
@Suppress("UNCHECKED_CAST", "SpellCheckingInspection")
class BPose(ptr: ConstructorData) : BlendData(ptr) {

    // chanbase: ListBase, *chanhash: GHash, **chan_array: bPoseChannel, flag: short, ctime: float,
    // stride_offset[3]: float, cyclic_offset[3]: float, agroups: ListBase, active_group: int, iksolver: int,
    // *ikdata: void, *ikparam: void, avs: bAnimVizSettings

    val time = float("ctime")
    val channels = inside("chanbase") as BListBase<BPoseChannel>

    override fun toString(): String {
        return "bPose { time: $time, channels: [${channels.joinToString { "\n  $it" }}\n] }"
    }
}