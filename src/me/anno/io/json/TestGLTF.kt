package me.anno.io.json

import de.javagl.jgltf.model.io.GltfModelReader
import java.io.File

fun main(){

    // todo gltf requests that we implement pbr...
    // todo should we redesign our whole pipeline; implement it partially, or ignore it?

    GltfModelReader().read(File("C:\\Users\\Antonio\\Documents\\redMonkey.glb").toURI())

}