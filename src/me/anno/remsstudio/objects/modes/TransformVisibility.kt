package me.anno.remsstudio.objects.modes

import me.anno.gpu.GFX.isFinalRendering
import me.anno.language.translation.NameDesc

enum class TransformVisibility(val id: Int, val naming: NameDesc){
    VISIBLE(0, NameDesc("Visible")){
        override val isVisible = true
    },
    EDITOR_ONLY(1, NameDesc("Editor Only")){
        override val isVisible: Boolean get() = !isFinalRendering
    },
    VIDEO_ONLY(2, NameDesc("Video Only")){
        override val isVisible: Boolean get() = isFinalRendering
    };
    abstract val isVisible: Boolean
    companion object {
        operator fun get(id: Int) = values().firstOrNull { id == it.id } ?: VISIBLE
    }
}