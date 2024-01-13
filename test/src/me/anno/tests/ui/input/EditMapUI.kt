package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.ecs.annotations.Type
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.studio.Inspectable
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.black3
import me.anno.utils.Color.white3
import org.joml.Vector3f

class EditMapUI : Saveable(), Inspectable {

    @Type("Map<Int,Color3>")
    var map = HashMap<Int, Vector3f>()

    @Type("Color3")
    var color = Vector3f()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }
}

fun main() {
    disableRenderDoc()
    val instance = EditMapUI()
    instance.map[0] = white3
    instance.map[14654] = black3
    testUI("EditMapUI", PropertyInspector({ instance }, style, Unit))
}