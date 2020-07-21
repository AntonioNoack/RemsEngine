package me.anno.studio.history

import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.studio.Studio.root

class SceneHState: HistoryState<Transform>(){

    fun setState(){
        state = root.clone()
    }

    override fun apply(state: Transform) {
        root = state
    }

    override fun writeState(writer: BaseWriter, name: String, v: Transform) {
        writer.writeObject(this, name, v)
    }

    override fun getClassName() = "SceneHState"

}