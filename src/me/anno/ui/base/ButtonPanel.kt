package me.anno.ui.base

import me.anno.ui.style.Style

open class ButtonPanel(text: String, style: Style): TextPanel(text, style.getChild("button"))