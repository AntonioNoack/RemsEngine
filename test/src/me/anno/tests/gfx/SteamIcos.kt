package me.anno.tests.gfx

import me.anno.config.DefaultConfig.style
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.base.buttons.ImageButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList2D
import me.anno.ui.debug.TestStudio.Companion.testUI3

fun main() {
    testUI3 {
        val list = PanelList2D(style)
        for (file in getReference("C:/Program Files (x86)/Steam/steam/games").listChildren() ?: emptyList()) {
            list.add(ImageButton(file, 512, Padding.Zero, true, style))
        }
        list
    }
}