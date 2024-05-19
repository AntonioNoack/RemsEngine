package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

class BAnimData(ptr: ConstructorData) : BlendData(ptr) {

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