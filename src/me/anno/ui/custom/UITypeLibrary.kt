package me.anno.ui.custom

open class UITypeLibrary(val typeList: MutableList<CustomPanelType>) {
    fun getType(name: String) = typeList.firstOrNull { it.internalName == name }
    fun createDefault() = typeList.first().generator()
}