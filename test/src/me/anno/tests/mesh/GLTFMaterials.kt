package me.anno.tests.mesh

import me.anno.mesh.gltf.GLTFMaterialExtractor
import me.anno.utils.OS

fun main() {
    println(GLTFMaterialExtractor.extract(OS.downloads.getChild("3d/azeria/Scene.gltf")))
    println(GLTFMaterialExtractor.extract(OS.downloads.getChild("3d/azeria/binary/azeria.glb")))
}