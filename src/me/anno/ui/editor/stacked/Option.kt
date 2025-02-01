package me.anno.ui.editor.stacked

import me.anno.language.translation.NameDesc

class Option<V>(val nameDesc: NameDesc, val generator: () -> V) {
    fun getSample() = generator()
}