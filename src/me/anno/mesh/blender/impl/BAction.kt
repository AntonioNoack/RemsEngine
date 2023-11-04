package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L236
 * */
@Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
class BAction(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID

    // markers could be very interesting for procedural things :)
    //  id: ID, curves: ListBase, chanbase: ListBase, groups: ListBase, markers: ListBase, flag: int, active_marker: int,
    //  idroot: int, frame_start: float, frame_end: float, *preview: PreviewImage

    val groups = inside("groups") as BListBase<BActionGroup>
    val curves = inside("curves") as BListBase<FCurve>
    val channels = inside("chanbase") as BListBase<BActionChannel>

    override fun toString(): String {
        return "bAction { $id, channels: $channels, groups: $groups, curves: [${curves.joinToString { "\n  $it" }}\n] }"
    }
}