package me.anno.objects.text

enum class TextRenderMode (val displayName: String, val id: Int){
    MESH("Mesh", 0),
    SDF("Signed Distance Field", 1),
    SDF_JOINED("Merged Signed Distance Field", 2)
}