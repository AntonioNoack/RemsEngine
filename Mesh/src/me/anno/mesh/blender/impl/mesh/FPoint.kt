package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_anim_types.h#L571
 * */
class FPoint(ptr: ConstructorData) : BlendData(ptr) {
    // vec[2]: float, flag: int, _pad[4]: char
}