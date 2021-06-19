package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.io.FileReference

// todo MeshComponent + MeshRenderComponent (+ AnimatableComponent) = animated skeleton
class MeshComponent: Component() {

    var file = FileReference()

    // todo ui components, because we want everything to be ecs -> reuse our existing stuff? maybe

    // todo in a game, there are assets, so
    // todo - we need to pack assets
    // todo - it would be nice, if FileReferences could point to local files as well
    // todo always ship the editor with the game? would make creating mods easier :)
    // (and cheating, but there always will be cheaters, soo...)

    // todo custom shading environment, so we can easily convert every shader into something clickable


    override fun getClassName(): String = "MeshComponent"

}