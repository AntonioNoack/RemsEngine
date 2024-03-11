package me.anno.graph.hdb.allocator

import me.anno.utils.InternalAPI

@InternalAPI
enum class ReplaceType {
    WriteCompletely,
    InsertInto,
    Append
}
