package me.anno.utils

import me.anno.gpu.GFX
import kotlin.math.abs


fun List<Int>.joinChars() = joinToString(""){ String(Character.toChars(it)) }

fun getIndexFromText(characters: List<Int>, localX: Float, fontName: String, textSize: Int, isBold: Boolean, isItalic: Boolean): Int {
    val list = BinarySearch.ExpensiveList(characters.size+1){
        if(it == 0) 0f
        else { GFX.getTextSize(fontName, textSize, isBold, isItalic, characters.subList(0, it).joinChars()).first.toFloat() }
    }
    var index = list.binarySearch { it.compareTo(localX) }
    if(index < 0) index = -1 - index
    // find the closer neighbor
    if(index > 0 && index < characters.size && abs(list[index-1]-localX) < abs(list[index]-localX)){
        index--
    }
    return index
}