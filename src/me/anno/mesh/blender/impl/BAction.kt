package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "UNCHECKED_CAST")
class BAction(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID

    //  id: ID, curves: ListBase, chanbase: ListBase, groups: ListBase, markers: ListBase, flag: int, active_marker: int,
    //  idroot: int, _pad[4]: char, frame_start: float, frame_end: float, *preview: PreviewImage

    val curves = inside("curves") as BListBase<FCurve>
    val channels = inside("chanbase") as BListBase<BActionChannel>

    override fun toString(): String {
        return "bAction { $id, channels: $channels, curves: $curves }"
    }
}