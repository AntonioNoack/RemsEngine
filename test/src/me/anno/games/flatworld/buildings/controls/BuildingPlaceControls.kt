package me.anno.games.flatworld.buildings.controls

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.input.Key
import me.anno.mesh.Shapes.flatCube
import me.anno.games.flatworld.FlatWorld
import me.anno.games.flatworld.buildings.LivingBuilding
import me.anno.utils.types.Floats.toRadians

class BuildingPlaceControls(val world: FlatWorld, rv: RenderView) : ControlScheme(rv) {

    // todo registry of types
    // todo UI to choose type
    val type = LivingBuilding(flatCube.scaled(5f).front)
    var rotationYDegrees = 0.0

    override fun onUpdate() {
        super.onUpdate()
        canBePlaced = false
        findHoveredBuildings()
    }

    fun findHoveredBuildings() {
        val query = RayQuery(renderView.cameraPosition, renderView.mouseDirection, 1e6)
        if (!Raycast.raycastClosestHit(world.scene, query)) {
            return
        }

        // check if hit is on terrain
        val isOnTerrain = query.result.component?.anyInHierarchy { it == world.terrain } == true
        if (!isOnTerrain) return

        // todo find whether building can be placed
        //  - check terrain for height
        //  - check terrain for other buildings
        //  - check terrain for streets
        canBePlaced = true

        transform.setLocal(
            transform.localTransform.identity()
                .translate(query.result.positionWS)
                .rotateY(rotationYDegrees.toRadians())
        )
        transform.teleportUpdate()

        // todo correct terrain until fitting on right press
    }

    var canBePlaced = true

    val comp = MeshComponent()
    val transform = Transform()

    override fun fill(pipeline: Pipeline) {
        // show current building
        if (!canBePlaced) return
        pipeline.addMesh(type.mesh, comp, transform)
        // todo show additionally connected roads
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (button == Key.BUTTON_LEFT && canBePlaced) {
            placeBuilding()
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (Input.isShiftDown) {
            rotationYDegrees += (dx + dy) * 5.0
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_PERIOD -> rotationYDegrees += 5.0
            Key.KEY_COMMA -> rotationYDegrees -= 5.0
            else -> super.onKeyTyped(x, y, key)
        }
    }

    fun placeBuilding() {
        val instance = type::class.constructors
            .first { it.parameters.size == 1 }
            .call(type.mesh)
        instance.position.set(transform.localPosition)
        instance.rotationYDegrees = rotationYDegrees
        val transform = Entity(world.buildings)
            .add(MeshComponent(type.mesh))
            .add(instance)
            .transform
        transform.setLocal(this.transform.localTransform)
        transform.teleportUpdate()
        world.buildingInstances.add(instance)
    }
}