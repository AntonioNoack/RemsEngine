package org.recast4j.recast

fun interface RecastBuilderProgressListener {
    fun onProgress(completed: Int, total: Int)
}