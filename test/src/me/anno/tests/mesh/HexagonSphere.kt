package me.anno.tests.mesh

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createFaceMesh
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createHexSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createLineMesh
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.input.IntInput

fun main() {
    var n = 4
    val lineMesh = Mesh()
    lineMesh.material = Material().apply { diffuseBase.set(0.5f, 1f, 0.5f) }.ref
    val faceMesh = Mesh()
    fun validate() {
        val hexagons = createHexSphere(n, true)
        createLineMesh(lineMesh, hexagons)
        createFaceMesh(faceMesh, hexagons)
    }
    validate()
    val entity = Entity()
    entity.add(MeshComponent(faceMesh.ref))
    val scaled = Entity()
    scaled.add(MeshComponent(lineMesh.ref))
    scaled.scale = scaled.scale.set(1.0001)
    entity.add(scaled)
    testUI2 {
        EditorState.prefabSource = entity.ref
        val main = SceneView(EditorState, PlayMode.EDITING, style)
        main.weight = 1f
        val controls = PanelListY(style)
        controls.add(IntInput("n", "", n, Type.LONG_PLUS, style)
            .setChangeListener {
                n = it.toInt()
                validate()
            })
        listOf(main, controls)
    }
}