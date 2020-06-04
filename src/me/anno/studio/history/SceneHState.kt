package me.anno.studio.history

import me.anno.gpu.GFX.root
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform

class SceneHState: HistoryState<Transform>(){

    fun setState(){
        state = root.clone()
    }

    override fun apply(state: Transform) {
        root = state
    }

    override fun writeState(writer: BaseWriter, name: String, v: Transform) {
        writer.writeObject(this, name, state)
    }

    override fun getClassName() = "SceneHState"

}