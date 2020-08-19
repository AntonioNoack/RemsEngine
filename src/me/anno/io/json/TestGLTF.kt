package me.anno.io.json

import de.javagl.jgltf.model.GltfModel
import de.javagl.jgltf.model.io.GltfModelReader
import de.javagl.jgltf.viewer.AbstractGltfViewer
import de.javagl.jgltf.viewer.DefaultRenderedGltfModel
import de.javagl.jgltf.viewer.lwjgl.GltfViewerLwjgl
import me.anno.utils.OS
import java.io.File

var inited = false
lateinit var model: GltfModel
lateinit var viewer: GltfViewerLwjgl
fun testModelRendering(){
    if(!inited){
        inited = true
        model = GltfModelReader().read(File(OS.documents, "redMonkey.glb").toURI())
        viewer = GltfViewerLwjgl()
        viewer.addGltfModel(model)
    }
    viewer.prepareRender()
    viewer.beforeRender()
    viewer.renderGltfModels()
}

fun testGLTF(){
    // todo gltf requests that we implement pbr...
    // todo should we redesign our whole pipeline; implement it partially, or ignore it?

    testModelRendering()

    // are the renderABLE models called renderED models??
    // RenderedGltfModel

    // val viewer = GltfViewerLwjgl()

    val model = GltfModelReader().read(File(OS.documents, "redMonkey.glb").toURI())
    model.sceneModels[0].nodeModels[0].meshModels[0].meshPrimitiveModels[0].indices
}

fun testObjectMapperJson(){

    class TestClass {
        // val intValue = 15
        // val maybeInt: Int? = null
        val intArray = intArrayOf(15, 17)
        val genericArray = arrayOf(intArray, 12, 3)
        // val mapExample = mapOf("key" to intValue)
    }

    // todo intArray inside genericArray isn't saved correctly
    val output = System.out
    ObjectMapper.writeValue(output, TestClass())
    println()

}

fun main(){

    testObjectMapperJson()

}