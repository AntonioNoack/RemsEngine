package me.anno.graph.hdb

data class HDBKey(val path: List<String>, val hash: Long) {
    companion object {
        val InvalidKey = HDBKey(emptyList(), 0)
    }
}