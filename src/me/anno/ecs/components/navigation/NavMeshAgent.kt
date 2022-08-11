package me.anno.ecs.components.navigation

import org.joml.Vector3f

class NavMeshAgent {

    // todo if null, find first best nav mesh
    var mesh: NavMesh? = null

    // todo given the mesh, find a good path

    var target = Vector3f()
        set(value) {
            field.set(value)
        }

    fun findDir(dst: Vector3f): Vector3f {
        // todo find path towards target
        return dst
    }

    // todo functions for jumps and links and such...

    // todo crowd agents will be useful in the future for multiple concurrent path finders

}