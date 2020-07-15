package me.anno.objects.effects

enum class ToneMappers(val displayName: String, val code: String){
    RAW("None", "raw"),
    REINHARD("Reinhard", "reinhard"),
    ACES("ACES", "aces"),
    UCHIMURA("Uchimura", "uchimura")
    // more could be added, but I'm happy with the current selection ;)
    // more tone mappers means that they need to be implemented as GLSL shaders,
    // and that they need an extra parameter in the GLSL code
}