package me.anno.studio.history

import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.studio.Studio.root
import me.anno.utils.size

class SceneState: HistoryState<Transform>(){

    fun update(){
        state = root.clone()
        // (state!!.listOfAll.size)
    }

    override fun apply(state: Transform) {
        root = state
    }

    override fun writeState(writer: BaseWriter, name: String, v: Transform) {
        writer.writeObject(this, name, v)
    }

    override fun getClassName() = "SceneHState"

}