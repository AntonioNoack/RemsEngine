package me.anno.ui.base.menu

class ComplexMenuGroup(
    title: String,
    description: String,
    isEnabled: Boolean,
    val children: List<ComplexMenuEntry>
) : ComplexMenuEntry(title, description, isEnabled)