package me.anno.objects.effects

enum class ToneMappers(val displayName: String, val code: String){
    RAW("None", "raw"),
    REINHARD("Reinhard", "reinhard"),
    ACES("ACES", "aces"),
    UCHIMURA("Uchimura", "uchimura")
}