package me.anno.graph.hdb.index

import me.anno.utils.InternalAPI

@InternalAPI
data class File(var lastAccessedMillis: Long, var range: IntRange)