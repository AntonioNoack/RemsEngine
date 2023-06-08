package me.anno.ui.base.menu

sealed class ComplexMenuEntry(
    val title: String,
    val description: String,
    val isEnabled: Boolean
)