package me.anno.ecs.components.light

import me.anno.ecs.Component

class PlanarReflection : Component() {

    // todo render the scene from the correct point of view
    // todo alternatively, just render the scene from our original point of view,
    // todo with a stencil mask maybe, and mirroring the points in the fragment shader

    // todo only render, if the front is visible

    // todo then display it:
    // todo a) as a decal, overriding glossy surfaces
    // todo b) as a mesh

}