package me.anno.ui.custom

import me.anno.ui.base.Panel

class Type(val displayName: String, val constructor: () -> Panel) {
    val internalName: String = constructor().javaClass.simpleName
}