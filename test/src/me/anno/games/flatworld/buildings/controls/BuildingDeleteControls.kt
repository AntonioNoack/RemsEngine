package me.anno.games.flatworld.buildings.controls

import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Key
import me.anno.games.flatworld.FlatWorld
import me.anno.games.flatworld.buildings.Building
import me.anno.games.flatworld.streets.controls.StreetDeletingControls
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d

class BuildingDeleteControls(val world: FlatWorld, rv: RenderView) : ControlScheme(rv) {

    override fun onUpdate() {
        super.onUpdate()
        selectedInstance = null
        findHoveredBuildings()
    }

    fun findHoveredBuildings() {
        val query = RayQuery(renderView.cameraPosition, Vector3d(renderView.mouseDirection), 1e6)
        if (!Raycast.raycast(world.scene, query)) return
        // find which instance is used...
        val instance = query.result.component?.getComponent(Building::class) ?: return
        selectedInstance = instance
        transform.localPosition = instance.position
        transform.localRotation = transform.localRotation
            .rotationY(instance.rotationYDegrees.toRadians())
        transform.setLocalScale(1.01f)
        transform.teleportUpdate()
    }

    var selectedInstance: Building? = null

    val comp = MeshComponent()
    val transform = Transform()

    init {
        comp.materials = StreetDeletingControls.deleteMaterialList
    }

    override fun fill(pipeline: Pipeline) {
        // todo show current building
        // todo show additionally connected roads
        val instance = selectedInstance ?: return
        pipeline.addMesh(instance.mesh, comp, transform)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (button == Key.BUTTON_LEFT) {
            deleteSelected()
        }
    }

    fun deleteSelected() {
        // if can delete, and sth is deletable, delete it
        val instance = selectedInstance ?: return
        instance.entity?.removeFromParent()
        world.buildingInstances.remove(instance)
    }
}