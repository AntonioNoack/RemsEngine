package me.anno.studio.history

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.windowStack
import me.anno.studio.rems.Selection
import me.anno.studio.rems.Selection.select
import me.anno.ui.editor.sceneView.SceneView
import me.anno.utils.Lists.join

class HistoryState() : Saveable() {

    constructor(title: String, code: Any): this(){
        this.title = title
        this.code = code
    }

    var title = ""
    var code: Any? = null

    lateinit var root: Transform
    var selectedUUID = -1L
    var selectedPropName: String? = null
    var usedCameras = LongArray(0)
    var editorTime = 0.0

    override fun hashCode(): Int {
        var result = root.toString().hashCode()
        result = 31 * result + selectedUUID.toInt()
        result = 31 * result + usedCameras.contentHashCode()
        result = 31 * result + editorTime.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is HistoryState &&
                other.selectedUUID == selectedUUID &&
                other.root.toString() == root.toString() &&
                other.usedCameras.contentEquals(usedCameras) &&
                other.editorTime == editorTime
    }

    fun apply() {
        RemsStudio.root = this.root
        RemsStudio.editorTime = editorTime
        val listOfAll = root.listOfAll.toList()
        select(selectedUUID, selectedPropName)
        windowStack.map { window ->
            window.panel.listOfAll.filterIsInstance<SceneView>().forEachIndexed { index, it ->
                it.camera = if (index in usedCameras.indices) {
                    val cameraIndex = usedCameras[index]
                    listOfAll.firstOrNull { camera -> camera.uuid == cameraIndex } as? Camera ?: nullCamera!!
                } else {
                    nullCamera!!
                }
            }
        }
        RemsStudio.updateSceneViews()
    }

    fun capture(previous: HistoryState?) {

        val state = this
        state.editorTime = RemsStudio.editorTime

        if (previous?.root?.toString() != RemsStudio.root.toString()) {
            // create a clone, if it was changed
            state.root = RemsStudio.root.clone()!!
        } else {
            // else, just reuse it; this is more memory and storage friendly
            state.root = previous.root
        }

        state.title = title
        state.selectedUUID = Selection.selectedTransform?.uuid ?: -1L
        state.usedCameras = windowStack.map { window ->
            window.panel.listOfAll.filterIsInstance<SceneView>().map { it.camera.uuid }.toList()
        }.join().toLongArray()

    }

    companion object {
        fun capture(title: String, code: Any, previous: HistoryState?): HistoryState {
            val state = HistoryState(title, code)
            state.capture(previous)
            return state
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "root", root)
        writer.writeString("title", title)
        writer.writeLong("selectedUUID", selectedUUID)
        writer.writeLongArray("usedCameras", usedCameras)
        writer.writeDouble("editorTime", editorTime)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "title" -> title = value
            else -> super.readString(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "editorTime" -> editorTime = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readLong(name: String, value: Long) {
        when (name) {
            "selectedUUID" -> selectedUUID = value
            else -> super.readLong(name, value)
        }
    }

    override fun readLongArray(name: String, value: LongArray) {
        when (name) {
            "usedCameras" -> usedCameras = value
            else -> super.readLongArray(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "root" -> root = value as? Transform ?: return
            else -> super.readObject(name, value)
        }
    }

    override fun getClassName() = "HistoryState"

    override fun getApproxSize(): Int = 1_000_000_000

    override fun isDefaultValue() = false

}