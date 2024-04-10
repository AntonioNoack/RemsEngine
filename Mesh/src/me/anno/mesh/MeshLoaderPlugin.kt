package me.anno.mesh

import me.anno.ecs.components.mesh.material.Material
import me.anno.extensions.plugins.Plugin
import me.anno.image.thumbs.AssetThumbnails
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.inner.InnerFolderCache
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
        Thumbs.registerSignature("blend", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignature("mitsuba-scene", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignature("mitsuba-meshes", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignature("maya", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("obj", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("fbx", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("gltf", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("glb", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("dae", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("md2", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("md5mesh", AssetThumbnails::generateAssetFrame)
        Thumbs.registerExtension("mtl") { srcFile, dstFile, size, callback ->
            // read as folder
            val children = InnerFolderCache.readAsFolder(srcFile, false)?.listChildren() ?: emptyList()
            if (children.isNotEmpty()) {
                val maxSize = 25 // with more, too many details are lost
                AssetThumbnails.generateMaterialFrame(
                    srcFile, dstFile,
                    if (children.size < maxSize) children else
                        children.subList(0, maxSize), size, callback
                )
            } else {
                // just an empty material to symbolize, that the file is empty
                // we maybe could do better with some kind of texture...
                AssetThumbnails.generateMaterialFrame(srcFile, dstFile, Material(), size, callback)
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