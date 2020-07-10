package me.anno.objects.meshes

import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.cache.Cache
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File
import java.lang.RuntimeException

class Mesh(var file: File, parent: Transform?): GFXTransform(parent){

    var data: MeshData? = null
    var lastFile: File? = null

    override fun onDraw(stack: Matrix4fArrayList, time: Float, color: Vector4f) {

        val file = file
        if(lastFile != file || data == null){
            // todo load the 3D model
            data = Cache.getEntry(file, false, "", 1000){
                // todo load the model...
                throw RuntimeException()
            } as MeshData
            lastFile = file
        }

        data?.draw(stack, time, color)

    }

}