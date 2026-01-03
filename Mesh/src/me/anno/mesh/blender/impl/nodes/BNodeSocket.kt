package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BLink
import me.anno.mesh.blender.impl.values.BNSValue

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
@Suppress("unused", "SpellCheckingInspection")
class BNodeSocket(ptr: ConstructorData) : BLink<BNodeSocket>(ptr) {

    val name = string("name", 64)
    val type = string("idname", 64) // e.g. NodeSocketShader, NodeSocketVector, NodeSocketFloat

    // used, when nothing is connected
    val defaultValue = getPointer("*default_value") as? BNSValue

    // all empty:
    // val label = string("label[64]", 64)
    // val shortLabel = string("short_label[64]", 64)
    // val description = string("description[64]", 64)

    override fun toString(): String {
        return "bNodeSocket { $name: $type, default: $defaultValue }"
    }

    // {*next=bNodeSocket(464)@0, *prev=bNodeSocket(464)@8, *prop=IDProperty(136)@16, identifier[64]=char(1)@24,
    // name[64]=char(1)@88, *storage=void(0)@152, type=short(2)@160, flag=short(2)@162, limit=short(2)@164,
    // in_out=short(2)@166, *typeinfo=bNodeSocketType(0)@168, idname[64]=char(1)@176,
    // *default_value=void(0)@240, stack_index=short(2)@248, display_shape=char(1)@250,
    // attribute_domain=char(1)@251, label[64]=char(1)@256, description[64]=char(1)@320,
    // *default_attribute_name=char(1)@384, own_index=int(4)@392, to_index=int(4)@396, *link=bNodeLink(56)@400,
    // ns=bNodeStack(48)@408, *runtime=bNodeSocketRuntimeHandle(0)@456}
}