package me.anno.studio.history

import me.anno.io.base.BaseWriter
import me.anno.ui.input.TextInput

class TextInputPanelHState(val panel: TextInput): HistoryState<String>(){

    fun setState(){
        state = panel.text
    }

    override fun apply(state: String) {
        panel.setText(state, false)
    }

    override fun writeState(writer: BaseWriter, name: String, v: String) {
        writer.writeString(name, v)
    }

    override fun getClassName() = "SceneHState"

}