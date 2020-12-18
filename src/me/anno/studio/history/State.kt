package me.anno.studio.history

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.windowStack
import me.anno.ui.editor.sceneView.SceneView
import me.anno.utils.Lists.join

class State : Saveable() {

    var title = ""
    lateinit var root: Transform
    var selectedTransform = -1
    var usedCameras = IntArray(0)
    var editorTime = 0.0

    override fun hashCode(): Int {
        var result = root.toString().hashCode()
        result = 31 * result + selectedTransform
        result = 31 * result + usedCameras.contentHashCode()
        result = 31 * result + editorTime.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is State &&
                other.selectedTransform == selectedTransform &&
                other.root.toString() == root.toString() &&
                other.usedCameras.contentEquals(usedCameras) &&
                other.editorTime == editorTime
    }

    fun apply() {
        RemsStudio.root = this.root
        RemsStudio.editorTime = editorTime
        val listOfAll = root.listOfAll.toList()
        RemsStudio.selectedTransform = listOfAll.getOrNull(selectedTransform)
        var index = 0
        windowStack.map { window ->
            window.panel.listOfAll.filterIsInstance<SceneView>().forEach {
                val camera = listOfAll.getOrNull(usedCameras.getOrNull(index++) ?: -1) as? Camera ?: nullCamera!!
                it.camera = camera
            }
        }
        RemsStudio.updateSceneViews()
    }

    fun capture(previous: State?){

        val state = this
        state.editorTime = RemsStudio.editorTime

        if(previous?.root?.toString() != RemsStudio.root.toString()){
            // create a clone, if it was changed
            state.root = RemsStudio.root.clone()!!
        } else {
            // else, just reuse it; this is more memory and storage friendly
            state.root = previous.root
        }

        state.title = title
        val listOfAll = RemsStudio.root.listOfAll.withIndex().associate { (index, it) -> it to index }
        state.selectedTransform = listOfAll[RemsStudio.selectedTransform] ?: -1
        state.usedCameras = windowStack.map { window ->
            window.panel.listOfAll.filterIsInstance<SceneView>().map { listOfAll[it.camera!!] ?: -1 }.toList()
        }.join().toIntArray()

    }

    companion object {
        fun capture(title: String, previous: State?): State {
            val state = State()
            state.title = title
            state.capture(previous)
            return state
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "root", root)
        writer.writeString("title", title)
        writer.writeInt("selectedTransform", selectedTransform)
        writer.writeIntArray("usedCameras", usedCameras)
        writer.writeDouble("editorTime", editorTime)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "title" -> title = name
            else -> super.readString(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "editorTime" -> editorTime = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "selectedTransform" -> selectedTransform = value
            else -> super.readInt(name, value)
        }
    }

    override fun readIntArray(name: String, value: IntArray) {
        when (name) {
            "usedCameras" -> usedCameras = value
            else -> super.readIntArray(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "root" -> root = value as? Transform ?: return
            else -> super.readObject(name, value)
        }
    }

    override fun getClassName() = "History2State"

    override fun getApproxSize(): Int = 1_000_000_000

    override fun isDefaultValue() = false

}