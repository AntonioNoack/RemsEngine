package me.anno.tests.ui.input

import me.anno.ui.input.NumberType
import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.engine.inspector.Inspectable
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.FloatVectorInput
import org.joml.Vector4f

fun main() {
    // setting an InputVector isn't possible in a property
    // -> is possible, so very weird that it's not working for me in my subproject...
    class Sample : Inspectable {
        override fun createInspector(
            inspected: List<Inspectable>,
            list: PanelListY,
            style: Style,
            getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
        ) {
            val vi = FloatVectorInput("Value", "", Vector4f(), NumberType.VEC4, DefaultConfig.style)
            list.add(vi)
            list.add(TextButton("Set 1,2,3,4", style).addLeftClickListener {
                vi.setValue(Vector4f(1f, 2f, 3f, 4f), true)
            })
            list.add(TextButton("Set 4,3,2,1", style).addLeftClickListener {
                vi.setValue(Vector4f(4f, 3f, 2f, 1f), true)
            })
        }
    }
    disableRenderDoc()
    val sample = Sample()
    testUI3("InputVector", PropertyInspector({ sample }, style, Unit))
}