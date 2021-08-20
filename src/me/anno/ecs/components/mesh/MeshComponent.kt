package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.shader.Shader
import me.anno.io.serialization.SerializedProperty
import me.anno.ui.editor.stacked.Option
import org.apache.logging.log4j.LogManager
import org.joml.AABBf


// todo (file) references to meshes and animations inside mesh files
//      - bird.fbx:anim:walk
//      - bird.fbx:mesh:wings
// todo static storage of things, e.g. for retargeting
// todo skeleton identity shall be defined by same names-array (for finding retargetings)

// todo drag item into inspector, and when clicking on something, let it show up the the last recently used? would allow to drag things from one to the other :3

// todo packages are mods/plugins, which are just installed with dependencies?

/////////////////////////////////////////////////////////////////////////////////////////
// todo MeshComponent + MeshRenderComponent (+ AnimatableComponent) = animated skeleton

// todo ui components, because we want everything to be ecs -> reuse our existing stuff? maybe

// todo in a game, there are assets, so
// todo - we need to pack assets
// todo - it would be nice, if FileReferences could point to local files as well
// todo always ship the editor with the game? would make creating mods easier :)
// (and cheating, but there always will be cheaters, soo...)

// todo custom shading environment, so we can easily convert every shader into something clickable
// todo also make it deferred / forward/backward compatible


class MeshComponent() : Component() {

    constructor(mesh: Mesh) : this() {
        this.mesh = mesh
    }

    @SerializedProperty
    var mesh: Mesh? = null

    var isInstanced = false

    var collisionMask: Int = 1

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    fun invalidate() {}

    // far into the future:
    // todo instanced animations for hundreds of humans:
    // todo bake animations into textures, and use indices + weights

    fun draw(shader: Shader, materialIndex: Int) {
        mesh?.draw(shader, materialIndex)
    }

    // on destroy we should maybe destroy the mesh:
    // only if it is unique, and owned by ourselves

    override fun clone(): PrefabSaveable {
        TODO("Not yet implemented")
    }

    override val className get() = "MeshComponent"

    companion object {

        private val LOGGER = LogManager.getLogger(MeshComponent::class)

    }

}