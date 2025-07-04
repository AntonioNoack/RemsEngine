package me.anno.mesh

import me.anno.ecs.components.mesh.material.Material
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.thumbs.AssetThumbnails
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.InnerFolderCache.readAsFolder
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.gltf.GLTFReader
import me.anno.mesh.maya.MayaASCII2015
import me.anno.mesh.mitsuba.MitsubaReader
import me.anno.mesh.obj.MTLReader
import me.anno.mesh.obj.OBJReader
import me.anno.utils.async.Callback

class MeshLoaderPlugin : Plugin() {
    override fun onEnable() {

        // read as folder
        InnerFolderCache.registerSignatures("gltf,json", GLTFReader::readAsFolder)
        InnerFolderCache.registerSignatures("fbx,dae,draco,md2,md5mesh,ply", AnimatedMeshesLoader::readAsFolder)
        InnerFolderCache.registerSignatures("blend", BlenderReader::readAsFolder)
        InnerFolderCache.registerSignatures("obj", OBJReader::readAsFolder)

        // their signatures are xml and json
        InnerFolderCache.registerFileExtensions("gltf", GLTFReader::readAsFolder)
        InnerFolderCache.registerFileExtensions("dae", AnimatedMeshesLoader::readAsFolder)
        InnerFolderCache.registerFileExtensions("obj", OBJReader::readAsFolder)
        InnerFolderCache.registerSignatures("mtl", MTLReader::readAsFolder)
        InnerFolderCache.registerSignatures("maya", MayaASCII2015::readAsFolder)
        InnerFolderCache.registerSignatures("mitsuba-meshes", MitsubaReader::readMeshesAsFolder)
        InnerFolderCache.registerSignatures("mitsuba-scene", MitsubaReader::readSceneAsFolder)

        // thumbnails
        ThumbnailCache.registerSignatures(
            "blend,mitsuba-scene,mitsuba-meshes,maya,obj,fbx,gltf,json,glb,dae,ply,md2,md5mesh",
            AssetThumbnails::generateAssetFrame
        )
        ThumbnailCache.registerFileExtensions("mtl", ::generateMTLThumbnail)
        ThumbnailCache.registerFileExtensions("obj", AssetThumbnails::generateAssetFrame)
    }

    private fun generateMTLThumbnail(
        srcFile: FileReference,
        dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        val children = readAsFolder(srcFile, false)?.listChildren() ?: emptyList()
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

    override fun onDisable() {
        InnerFolderCache.unregisterSignatures(
            "fbx,gltf,dae,draco,md2,md5mesh,blend,ply," +
                    "obj,mtl,maya,mitsuba-meshes,mitsuba-scene,json"
        )
    }
}