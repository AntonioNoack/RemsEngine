package me.anno.mesh

import me.anno.ecs.components.mesh.Material
import me.anno.extensions.plugins.Plugin
import me.anno.io.files.inner.InnerFolderCache
import me.anno.image.thumbs.Thumbs
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.maya.MayaASCII2015
import me.anno.mesh.mitsuba.MitsubaReader
import me.anno.mesh.obj.MTLReader
import me.anno.mesh.obj.OBJReader

class MeshLoaderPlugin : Plugin() {
    override fun onEnable() {

        // read as folder
        InnerFolderCache.register(listOf("fbx", "gltf", "dae", "draco", "md2", "md5mesh")) { it, c ->
            c.ok(AnimatedMeshesLoader.readAsFolder(it))
        }
        InnerFolderCache.register("blend", BlenderReader::readAsFolder)
        InnerFolderCache.register("obj", OBJReader.Companion::readAsFolder)
        InnerFolderCache.register("mtl", MTLReader.Companion::readAsFolder)
        InnerFolderCache.register("maya", MayaASCII2015::readAsFolder)
        InnerFolderCache.register("mitsuba-meshes", MitsubaReader::readMeshesAsFolder)
        InnerFolderCache.register("mitsuba-scene", MitsubaReader::readSceneAsFolder)

        // thumbnails
        Thumbs.registerSignature("blend", Thumbs::generateSomething)
        Thumbs.registerSignature("mitsuba-scene", Thumbs::generateSomething)
        Thumbs.registerSignature("mitsuba-meshes", Thumbs::generateSomething)
        Thumbs.registerSignature("maya", Thumbs::generateSomething)
        Thumbs.registerExtension("obj", Thumbs::generateSomething)
        Thumbs.registerExtension("fbx", Thumbs::generateSomething)
        Thumbs.registerExtension("gltf", Thumbs::generateSomething)
        Thumbs.registerExtension("glb", Thumbs::generateSomething)
        Thumbs.registerExtension("dae", Thumbs::generateSomething)
        Thumbs.registerExtension("md2", Thumbs::generateSomething)
        Thumbs.registerExtension("md5mesh", Thumbs::generateSomething)
        Thumbs.registerExtension("mtl") { srcFile, dstFile, size, callback ->
            // read as folder
            val children = InnerFolderCache.readAsFolder(srcFile, false)?.listChildren() ?: emptyList()
            if (children.isNotEmpty()) {
                val maxSize = 25 // with more, too many details are lost
                Thumbs.generateMaterialFrame(
                    srcFile, dstFile,
                    if (children.size < maxSize) children else
                        children.subList(0, maxSize), size, callback
                )
            } else {
                // just an empty material to symbolize, that the file is empty
                // we maybe could do better with some kind of texture...
                Thumbs.generateMaterialFrame(srcFile, dstFile, Material(), size, callback)
            }
        }
    }

    override fun onDisable() {
        for (ext in listOf(
            "fbx", "gltf", "dae", "draco", "md2",
            "md5mesh", "blend", "obj", "mtl",
            "maya", "mitsuba-meshes", "mitsuba-scene"
        )) InnerFolderCache.unregisterSignatures(ext)
    }
}