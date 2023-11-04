package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

// armature pose
/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L495
 * */
@Suppress("UNCHECKED_CAST", "SpellCheckingInspection")
class BPose(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    // chanbase: ListBase, *chanhash: GHash, **chan_array: bPoseChannel, flag: short, _pad[2]: char, ctime: float,
    // stride_offset[3]: float, cyclic_offset[3]: float, agroups: ListBase, active_group: int, iksolver: int,
    // *ikdata: void, *ikparam: void, avs: bAnimVizSettings

    val time = float("ctime")
    val channels = inside("chanbase") as BListBase<BPoseChannel>

    override fun toString(): String {
        return "bPose { time: $time, channels: $channels }"
    }
}