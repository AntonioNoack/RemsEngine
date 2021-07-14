package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.shader.Shader
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

    var mesh: Mesh? = null

    fun invalidate() {}

    // far into the future:
    // todo instanced animations for hundreds of humans:
    // todo bake animations into textures, and use indices + weights

    fun draw(shader: Shader, materialIndex: Int) {
        mesh?.draw(shader, materialIndex)
    }

    override fun onDestroy() {
        super.onDestroy()
        // buffer?.destroy()
    }

    override val className get() = "MeshComponent"

    companion object {

        private val LOGGER = LogManager.getLogger(MeshComponent::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4
        val attributes = listOf(
            Attribute("coords", 3),
            Attribute("uvs", 2),
            Attribute("normals", 3),
            Attribute("colors", 4),
            Attribute("weights", MAX_WEIGHTS),
            Attribute("indices", AttributeType.UINT8, MAX_WEIGHTS, true)
        )

        fun AABBf.clear() {
            minX = Float.POSITIVE_INFINITY
            minY = Float.POSITIVE_INFINITY
            minZ = Float.POSITIVE_INFINITY
            maxX = Float.NEGATIVE_INFINITY
            maxY = Float.NEGATIVE_INFINITY
            maxZ = Float.NEGATIVE_INFINITY
        }

    }

}