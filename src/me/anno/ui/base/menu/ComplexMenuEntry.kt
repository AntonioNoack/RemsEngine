package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

sealed class ComplexMenuEntry(
    val nameDesc: NameDesc,
    val isEnabled: Boolean
)