package me.anno.ui.custom

/**
 * stores, which panels shall be open-able within a CustomContainer
 * */
open class UITypeLibrary(val typeList: MutableList<CustomPanelType>) {
    fun getType(name: String) = typeList.firstOrNull { it.internalName == name }
    fun createDefault() = typeList.first().generator()
}