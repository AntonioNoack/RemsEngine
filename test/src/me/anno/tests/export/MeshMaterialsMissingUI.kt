package me.anno.tests.export

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.input.AnyArrayPanel
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.any2

fun main() {
    registerCustomClass(Mesh::class)
    registerCustomClass(Material::class)
    val sample = DefaultAssets.flatCube
    val pi = PrefabInspector(sample.ref)
    val list = PanelListY(style)
    val reflections = sample.getReflections()
    val properties = reflections.propertiesByClass.flatMap { it.second }
    assertTrue(properties.any2 { it.name == "materials" }) {
        properties.map { it.name }.toString()
    }
    assertTrue(properties.filter { it.name == "materials" }.all { it.serialize }) {
        properties.map { "${it.name}: ${it.serialize}" }.toString()
    }
    pi.inspect(listOf(sample), list, style)
    list.printLayout(0)
    assertTrue(list.listOfAll.any2 {
        it is AnyArrayPanel && it.title.name == "Materials"
    })
}