package me.anno.recast

import me.anno.extensions.plugins.Plugin
import me.anno.io.ISaveable.Companion.registerCustomClass

class RecastPlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerCustomClass(NavMesh::class)
        registerCustomClass(NavMeshAgent::class)
    }
}
