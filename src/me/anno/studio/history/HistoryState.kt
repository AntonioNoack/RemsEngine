package me.anno.studio.history

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.defaultWindowStack
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.Selection
import me.anno.studio.rems.Selection.select
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.editor.sceneView.StudioSceneView
import me.anno.utils.structures.lists.Lists.join

class HistoryState() : Saveable() {

    constructor(title: String, code: Any) : this() {
        this.title = title
        this.code = code
    }

    var title = ""
    var code: Any? = null

    var root: Transform? = null
    var selectedUUID = -1
    var selectedPropName: String? = null
    var usedCameras = IntArray(0)
    var editorTime = 0.0

    override fun hashCode(): Int {
        var result = root.toString().hashCode()
        result = 31 * result + selectedUUID
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

    fun Transform.getRoot(): Transform = this.parent?.getRoot() ?: this
    fun Transform.getUUID() = this.getRoot().listOfAll.indexOf(this)

    fun apply() {
        val root = root ?: return
        RemsStudio.root = root
        SceneTabs.currentTab?.scene = root
        RemsStudio.editorTime = editorTime
        val listOfAll = root.listOfAll.toList()
        select(selectedUUID, selectedPropName)
        defaultWindowStack?.forEach { window ->
            var index = 0
            window.panel.forAll {
                if (it is StudioSceneView) {
                    it.camera = if (index in usedCameras.indices) {
                        val cameraIndex = usedCameras[index]
                        listOfAll.firstOrNull { camera -> camera.getUUID() == cameraIndex } as? Camera ?: nullCamera!!
                    } else {
                        nullCamera!!
                    }
                    index++
                }
            }
        }
        invalidateUI()
    }

    fun capture(previous: HistoryState?) {

        val state = this
        state.editorTime = RemsStudio.editorTime

        if (previous?.root?.toString() != RemsStudio.root.toString()) {
            // create a clone, if it was changed
            state.root = RemsStudio.root.clone()
        } else {
            // else, just reuse it; this is more memory and storage friendly
            state.root = previous.root
        }

        state.title = title
        state.selectedUUID = Selection.selectedTransform?.getUUID() ?: -1
        state.usedCameras = defaultWindowStack!!.map { window ->
            window.panel.listOfAll.filterIsInstance<StudioSceneView>().map { it.camera.getUUID() }.toList()
        }.join().toIntArray()

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
        writer.writeInt("selectedUUID", selectedUUID)
        writer.writeIntArray("usedCameras", usedCameras)
        writer.writeDouble("editorTime", editorTime)
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "title" -> title = value ?: ""
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
            "selectedUUID" -> selectedUUID = value
            else -> super.readInt(name, value)
        }
    }

    override fun readLong(name: String, value: Long) {
        when (name) {
            "selectedUUID" -> selectedUUID = value.toInt()
            else -> super.readLong(name, value)
        }
    }

    override fun readIntArray(name: String, values: IntArray) {
        when (name) {
            "usedCameras" -> usedCameras = values
            else -> super.readIntArray(name, values)
        }
    }

    override fun readLongArray(name: String, values: LongArray) {
        when (name) {
            "usedCameras" -> usedCameras = values.map { it.toInt() }.toIntArray()
            else -> super.readLongArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "root" -> root = value as? Transform ?: return
            else -> super.readObject(name, value)
        }
    }

    override val className get() = "HistoryState"

    override val approxSize get() = 1_000_000_000

    override fun isDefaultValue() = false

}