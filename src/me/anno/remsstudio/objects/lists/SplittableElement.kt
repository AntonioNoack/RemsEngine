package me.anno.remsstudio.objects.lists

// todo start index, end index, step

interface SplittableElement {

    fun getSplittingModes(): List<String>? = null
    fun getSplitElement(mode: String, index: Int): Element
    fun getSplitLength(mode: String): Int

}