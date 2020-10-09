package me.anno.objects.modes

import me.anno.gpu.GFX.isFinalRendering

enum class TransformVisibility(val id: Int, val displayName: String){
    VISIBLE(0, "Visible"){
        override val isVisible = true
    },
    EDITOR_ONLY(1, "Editor Only"){
        override val isVisible: Boolean get() = !isFinalRendering
    },
    VIDEO_ONLY(2, "Video Only"){
        override val isVisible: Boolean get() = isFinalRendering
    };
    abstract val isVisible: Boolean
    companion object {
        operator fun get(id: Int) = values().firstOrNull { id == it.id } ?: VISIBLE
    }
}