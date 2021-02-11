package me.anno.objects.modes

import me.anno.language.translation.NameDesc

enum class TextRenderMode (val naming: NameDesc, val id: Int){
    MESH(NameDesc("Mesh"), 0),
    SDF(NameDesc("Signed Distance Field"), 1),
    SDF_JOINED(NameDesc("Merged Signed Distance Field"), 2)
}