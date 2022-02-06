package me.anno.remsstudio.objects.effects

import me.anno.language.translation.NameDesc

enum class ToneMappers(val id: Int,
                       val naming: NameDesc,
                       val glslFuncName: String){
    RAW8(4, NameDesc("None, 8 Bit"), ""),
    RAW(0, NameDesc("None, 32 Bit"), ""),
    REINHARD(1, NameDesc("Reinhard"), "reinhard"),
    ACES(2, NameDesc("ACES"), "aces"),
    UCHIMURA(3, NameDesc("Uchimura"), "uchimura")
    // more could be added, but I'm happy with the current selection ;)
    // more tone mappers means that they need to be implemented as GLSL shaders,
    // and that they need an extra parameter in the GLSL code
}