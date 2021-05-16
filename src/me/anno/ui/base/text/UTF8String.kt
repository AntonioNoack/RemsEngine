package me.anno.ui.base.text

import me.anno.utils.types.Strings.joinChars
import kotlin.streams.toList

class UTF8String {

    var text = ""
    var codePoints: List<Int> = emptyList()

    constructor()
    constructor(codePoints: List<Int>) {
        text = codePoints.joinChars()
        this.codePoints = codePoints
    }

    constructor(str: String) {
        text = str
        codePoints = str.codePoints().toList()
    }

}