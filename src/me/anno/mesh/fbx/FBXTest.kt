package me.anno.mesh.fbx

fun markCalculatorForJasmin() {

    val marks = 15..25
    val markCount = 15..19

    fun line() {
        for (mark in markCount) {
            print("------------------------")
        }
        println("----------")
    }

    print("    |  ")
    for (mark in markCount) {
        print("          $mark            ")
    }
    println()
    line()
    fun format(ones: Int, twos: Int, threes: Int) {
        // val durchschnitt = (einsen + zweien * 2 + dreien * 3) * 1f / (einsen + zweien + dreien)
        //  (${durchschnitt.f2()})
        print("${ones.f2xI(1)}${ones.p()} ${twos.f2xI(2)}${threes.p()} ${threes.f2xI(3)} | ")
    }
    for (mark in marks) {
        if (mark % 10 < 2) line()
        if (mark % 10 == 0) {
            print("${mark / 10},0 | ")
            for (markC in markCount) {
                when (mark) {
                    10 -> format(markC, 0, 0)
                    20 -> format(0, markC, 0)
                    30 -> format(0, 0, markC)
                }
            }
            println()
        } else {
            val mark2 = mark * 0.1
            print("${mark / 10},${mark % 10} | ")
            for (markC in markCount) {
                val rest = mark2 + 0.0499 - (mark / 10)
                val twos = (rest * markC).toInt()
                val ones = markC - twos
                if (mark > 20) {
                    format(0, ones, twos)
                } else {
                    format(ones, twos, 0)
                }
            }
            println()
        }
    }
}

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

    markCalculatorForJasmin()

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
        println(geometry)
        val vertices = geometry["Vertices"].first().properties[0] as DoubleArray
        val vertexIndices = geometry["PolygonVertexIndex"].first().properties[0] as IntArray
        println("geometry with ${vertices.size} vertices, and ${vertexIndices.size} indices")
        val materialIndices = geometry["LayerElementMaterial"].firstOrNull()?.get("Materials")?.get(0)?.properties?.get(0) as? IntArray
        // crazy numbers...
        println("materials: ${materialIndices?.joinToString()}")
        val colors = geometry["LayerElementColor"].firstOrNull()?.get("Colors")?.get(0)?.properties?.get(0) as? DoubleArray // crazy values as well :/
        println("colors: ${colors?.joinToString()}")

    }
    println(objects.children.size)
    println(nodeAttributes.size)*/
}