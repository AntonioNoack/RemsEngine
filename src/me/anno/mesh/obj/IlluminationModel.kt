package me.anno.mesh.obj

/**
 * TR = transparency,
 * FR = fresnel, RT = ray tracing (not supported)
 * RF = refraction, GS = glass

    0. Color on and Ambient off
    1. Color on and Ambient on
    2. Highlight on
    3. Reflection on and Ray trace on
    4. Transparency: Glass on, Reflection: Raytrace on
    5. Reflection: Fresnel on and Ray trace on
    6. Transparency: Refraction on, Reflection: Fresnel off and Ray trace on
    7. Transparency: Refraction on, Reflection: Fresnel on and Ray trace on
    8. Reflection on and Ray trace off
    9. Transparency: Glass on, Reflection: Raytrace off
    10. It casts shadows onto invisible surfaces

 * */
enum class IlluminationModel(val id: Int){
    COLOR(0),
    COLOR_WITH_AMBIENT(1),
    HIGHLIGHT(2),
    // give them proper names, when they are implemented; they are pretty complex (see the table)
    S3(3), S4(4), S5(5), S6(6), S7(7), S8(8), S9(9),
    S10(10)
}