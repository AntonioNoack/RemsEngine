package me.anno.graph.visual.control

import me.anno.io.saveable.Saveable

class EmptyState: Saveable() {
    companion object {
        val INSTANCE = EmptyState()
    }
}