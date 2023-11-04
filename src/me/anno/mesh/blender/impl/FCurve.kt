package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class FCurve(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<FCurve>(file, type, buffer, position) {

    // *next: FCurve, *prev: FCurve, *grp: bActionGroup, *driver: ChannelDriver, modifiers: ListBase, *bezt: BezTriple,
    // *fpt: FPoint, totvert: int, active_keyframe_index: int, curval: float, flag: short, extend: short,
    // auto_smoothing: char, _pad[3]: char, array_index: int, *rna_path: char, color_mode: int,
    // color[3]: float, prev_norm_factor: float, prev_offset: float
}