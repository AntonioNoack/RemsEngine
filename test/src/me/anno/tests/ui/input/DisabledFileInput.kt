package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveableProperty
import me.anno.engine.ui.input.ComponentUI
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.InputPanel

fun main() {
    disableRenderDoc()
    val instance = Mesh()
    instance.materials = listOf(Material().ref)
    val pi = PrefabInspector(instance.ref)
    val property = PrefabSaveableProperty(pi, listOf(instance), "materials", instance.getReflections()["materials"]!!)
    val ui = ComponentUI.createUI2("Materials", "", property, null, style)!!
    testUI("Disabled FileInput", ui.apply {
        forAllPanels { p ->
            (p as? InputPanel<*>)?.isInputAllowed = false
        }
    })
}