package me.anno.graph.hdb.index

import me.anno.utils.InternalAPI

@InternalAPI
class File(var lastAccessedMillis: Long, var range: IntRange)