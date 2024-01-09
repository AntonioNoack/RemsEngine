package me.anno.mesh

import me.anno.extensions.plugins.Plugin
import me.anno.io.files.inner.InnerFolderCache
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.maya.MayaASCII2015
import me.anno.mesh.mitsuba.MitsubaReader
import me.anno.mesh.obj.MTLReader
import me.anno.mesh.obj.OBJReader

class MeshLoaderPlugin : Plugin() {
    override fun onEnable() {
        InnerFolderCache.register(listOf("fbx", "gltf", "dae", "draco", "md2", "md5mesh")) { it, c ->
            c(AnimatedMeshesLoader.readAsFolder(it), null)
        }
        InnerFolderCache.register("blend", BlenderReader::readAsFolder)
        InnerFolderCache.register("obj", OBJReader.Companion::readAsFolder)
        InnerFolderCache.register("mtl", MTLReader.Companion::readAsFolder)
        InnerFolderCache.register("maya", MayaASCII2015::readAsFolder)
        InnerFolderCache.register("mitsuba-meshes", MitsubaReader::readMeshesAsFolder)
        InnerFolderCache.register("mitsuba-scene", MitsubaReader::readSceneAsFolder)
    }

    override fun onDisable() {
        for (ext in listOf(
            "fbx", "gltf", "dae", "draco", "md2",
            "md5mesh", "blend", "obj", "mtl",
            "maya", "mitsuba-meshes", "mitsuba-scene"
        )) InnerFolderCache.unregister(ext)
    }
}