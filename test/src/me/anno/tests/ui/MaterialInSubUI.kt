package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.graph.ui.GraphPanel.Companion.greenish
import me.anno.graph.ui.GraphPanel.Companion.red
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.PropertyInspector

fun main() {
    // todo make Materials editable directly, not via "Open As Scene"...
    // todo material list entries don't need to be nullable

    // todo index sample project
    // a few temporary files; cannot be GCed, because scope isn't left at runtime
    val green = Material.diffuse(greenish).ref
    val red = Material.diffuse(red).ref
    val tested = MeshComponent(flatCube.front, Material())
    testUI("Easy Material Editing", PropertyInspector({ tested }, style, Unit))
}