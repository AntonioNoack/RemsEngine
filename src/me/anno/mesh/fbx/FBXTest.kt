package me.anno.mesh.fbx

fun Int.p() = if (this == 0) " " else ","

fun Int.f2xI(i: Int) = if (this == 0) "      " else "${this.f2()} x $i"
fun Int.f2() = "${if (this > 9) (this / 10).toString() else " "}${this % 10}"

fun invertShort(l: UShort): UShort {
    val v = l.toUInt()
    return (v.shr(8) + v.shl(8)).toUShort()
}

fun invertInt(l: UInt): UInt {
    return (invertShort(l.shr(16).toUShort())
        .toUInt() + invertShort(l.toUShort()).toUInt().shl(16))
}

fun invertLong(l: ULong): ULong {
    return (invertInt(l.shr(32).toUInt())
        .toULong() + invertInt(l.toUInt()).toULong().shl(32))
}

fun main() {

    /*val root = */
    /*FBXReader(
        File(
            OS.downloads,
            "Warrior with animation.fbx"
        ).inputStream()
    )*/

    //ConvertMeshes.convertMeshes(File("C:\\Users\\Antonio\\Documents\\IdeaProjects\\HomeDesigner"), true)
    //val src = File(OS.documents, "IdeaProjects\\HomeDesigner\\models\\interior\\kitchen\\SM_Prop_Fridge_01.fbx")
    //ConvertMeshes.convertMeshes(src, src, true)

    // FBXLoader.loadFBX(File(OS.documents, "Bricks/2x4.fbx").inputStream().buffered())

    /*val objects = root["Objects"].first()
    val nodeAttributes = objects["NodeAttribute"]
    val models = objects["Model"]
    val poses = objects["Pose"]
    val materials = objects["Material"]
    val deformers = objects["Deformer"]
    val videos = objects["Video"] // ???, contains images
    val textures = objects["Texture"]
    val animations = objects["AnimationStack"]
    val animationLayers = objects["AnimationLayer"]
    val geometries = objects["Geometry"]
    geometries.forEach { geometry ->
        LOGGER.info(geometry)
        val vertices = geometry["Vertices"].first().properties[0] as DoubleArray
        val vertexIndices = geometry["PolygonVertexIndex"].first().properties[0] as IntArray
        LOGGER.info("geometry with ${vertices.size} vertices, and ${vertexIndices.size} indices")
        val materialIndices = geometry["LayerElementMaterial"].firstOrNull()?.get("Materials")?.get(0)?.properties?.get(0) as? IntArray
        // crazy numbers...
        LOGGER.info("materials: ${materialIndices?.joinToString()}")
        val colors = geometry["LayerElementColor"].firstOrNull()?.get("Colors")?.get(0)?.properties?.get(0) as? DoubleArray // crazy values as well :/
        LOGGER.info("colors: ${colors?.joinToString()}")

    }
    LOGGER.info(objects.children.size)
    LOGGER.info(nodeAttributes.size)*/
}