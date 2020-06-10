package me.anno.ui.input

import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.AudioLinePanel
import me.anno.ui.style.Style
import java.io.File

class AudioInput(file: File, style: Style): PanelListY(style){

    val pathInput = FileInput("Path", style, file.toString())
    val display = AudioLinePanel(style)

    init {
        this += pathInput
        this += display
    }

}