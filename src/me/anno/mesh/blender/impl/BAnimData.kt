package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BAnimData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    /*
    * *action: bAction, *tmpact: bAction, nla_tracks: ListBase, *act_track: NlaTrack, *actstrip: NlaStrip,
    * drivers: ListBase, overrides: ListBase, **driver_array: FCurve, flag: int,
    * act_blendmode: short, act_extendmode: short, act_influence: float
    * */

    val action = getPointer("*action") as? BAction

    override fun toString(): String {
        return "AnimData { $action }"
    }
}