package me.anno.utils

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.ui.base.TextPanel
import kotlin.math.abs
import kotlin.math.min


fun List<Int>.joinChars() = joinToString(""){ String(Character.toChars(it)) }

fun getLineWidth(line: List<Int>, endIndex: Int, tp: TextPanel) =
    getLineWidth(line, endIndex, tp.fontName, tp.textSize, tp.isBold, tp.isItalic)

fun getLineWidth(line: List<Int>, endIndex: Int, fontName: String, textSize: Int, isBold: Boolean, isItalic: Boolean): Float {
    return if(endIndex == 0) 0f
    else GFX.getTextSize(fontName, textSize, isBold, isItalic, line.subList(0, min(endIndex, line.size)).joinChars()).first.toFloat()
}

fun getIndexFromText(characters: List<Int>, localX: Float, tp: TextPanel) =
    getIndexFromText(characters, localX, tp.fontName, tp.textSize, tp.isBold, tp.isItalic)

fun getIndexFromText(characters: List<Int>, localX: Float, fontName: String, textSize: Int, isBold: Boolean, isItalic: Boolean): Int {
    val list = BinarySearch.ExpensiveList(characters.size+1){
        getLineWidth(characters, it, fontName, textSize, isBold, isItalic)
    }
    var index = list.binarySearch { it.compareTo(localX) }
    if(index < 0) index = -1 - index
    // find the closer neighbor
    if(index > 0 && index < characters.size && abs(list[index-1]-localX) < abs(list[index]-localX)){
        index--
    }
    return index
}


fun String.getImportType(): String =
    DefaultConfig["import.mapping.$this"]?.toString() ?:
    DefaultConfig["import.mapping.${toLowerCase()}"]?.toString() ?:
    DefaultConfig["import.mapping.*"]?.toString() ?: "Text"