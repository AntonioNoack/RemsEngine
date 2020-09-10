package me.anno.objects.effects

enum class ToneMappers(val id: Int, val displayName: String, val code: String){
    RAW(0,"None", "raw"),
    REINHARD(1,"Reinhard", "reinhard"),
    ACES(2,"ACES", "aces"),
    UCHIMURA(3,"Uchimura", "uchimura")
    // more could be added, but I'm happy with the current selection ;)
    // more tone mappers means that they need to be implemented as GLSL shaders,
    // and that they need an extra parameter in the GLSL code
}