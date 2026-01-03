package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BCustomLayerType.Companion.idToValue

@Suppress("unused")
class BCustomDataLayer(ptr: ConstructorData) : BlendData(ptr) {

    val type = i32("type")

    // val offset = int("offset")
    // val active = int("active")
    // val uid = int("uid")
    val name get() = string("name[64]", 64)
    val data = getInstantList<BlendData>("*data") ?: BInstantList.emptyList()

    override fun toString(): String {
        return "BCustomDataLayer { '$name', type: ${
            run {
                val data = data as? BInstantList<*>
                if (data?.instance == null) "#$type"
                else data.instance::class.simpleName
            }
        }, type#2: ${idToValue[type]}, data: ${data.size}x, sample: ${data.firstOrNull()} }"
    }
}