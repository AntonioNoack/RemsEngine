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
        InnerFolderCache.registerSignatures("fbx,gltf,dae,draco,md2,md5mesh") { it, c ->
            c.ok(AnimatedMeshesLoader.readAsFolder(it))
        }
        InnerFolderCache.registerSignatures("blend", BlenderReader::readAsFolder)
        InnerFolderCache.registerSignatures("obj", OBJReader.Companion::readAsFolder)
        InnerFolderCache.registerSignatures("mtl", MTLReader.Companion::readAsFolder)
        InnerFolderCache.registerSignatures("maya", MayaASCII2015::readAsFolder)
        InnerFolderCache.registerSignatures("mitsuba-meshes", MitsubaReader::readMeshesAsFolder)
        InnerFolderCache.registerSignatures("mitsuba-scene", MitsubaReader::readSceneAsFolder)

        // thumbnails
        Thumbs.registerSignatures("blend", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignatures("mitsuba-scene", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignatures("mitsuba-meshes", AssetThumbnails::generateAssetFrame)
        Thumbs.registerSignatures("maya", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("obj", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("fbx", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("gltf", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("glb", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("dae", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("md2", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("md5mesh", AssetThumbnails::generateAssetFrame)
        Thumbs.registerFileExtensions("mtl") { srcFile, dstFile, size, callback ->
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
        InnerFolderCache.unregisterSignatures(
            "fbx,gltf,dae,draco,md2,md5mesh,blend," +
                    "obj,mtl,maya,mitsuba-meshes,mitsuba-scene"
        )
    }
}