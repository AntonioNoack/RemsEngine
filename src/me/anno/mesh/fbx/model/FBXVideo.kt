package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

// can be a texture as well
class FBXVideo(data: FBXNode): FBXObject(data) {

    var path = data.getProperty("Filename") as? String
    var relPath = data.getProperty("RelativeFilename") as? String
    val useMipmapping = data.getBoolean("UseMipMap") ?: false

    override fun onReadProperty70(name: String, value: Any) {
        when(name){
            // somehow they exist twice...
            "Path" -> path = value as String
            "RelPath" -> relPath = value as String
            else -> {
                // framerate, width, mipmapping, use system framerate + framerate override
            }
        }
    }

}