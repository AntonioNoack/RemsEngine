package me.anno.ui.custom

open class UITypeLibrary(val typeList: MutableList<Type>) {
    val types get() = typeList.associateBy { it.internalName }
    fun createDefault() = types.entries.first().value.constructor()
}