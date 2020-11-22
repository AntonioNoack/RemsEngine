package me.anno.mesh.fbx

import me.anno.mesh.fbx.structure.FBXReader
import me.anno.utils.OS
import java.io.File

/*fun notenRechnerFuerJasmin(){

    val notenZahlen = 15 .. 19

    fun line(){
        for(notenZahl in notenZahlen){
            print("------------------------")
        }
        println("----------")
    }

    print("    |  ")
    for(notenZahl in notenZahlen){
        print("          $notenZahl            ")
    }
    println()
    line()
    fun format(einsen: Int, zweien: Int, dreien: Int){
        // val durchschnitt = (einsen + zweien * 2 + dreien * 3) * 1f / (einsen + zweien + dreien)
        //  (${durchschnitt.f2()})
        print("${einsen.f2xI(1)}${einsen.p()} ${zweien.f2xI(2)}${dreien.p()} ${dreien.f2xI(3)} | ")
    }
    for(noteX10 in 15 .. 19){
        val note = noteX10 * 0.1
        print("${noteX10/10},${noteX10%10} | ")
        for(notenZahl in notenZahlen){
            val rest = note + 0.0499 - 1.0
            val zweien = (rest * notenZahl).toInt()
            val einsen = notenZahl - zweien
            //  (${((einsen + zweien + zweien).toFloat()/notenZahl).f3()})
            format(einsen, zweien, 0)
        }
        println()
    }
    line()
    for(noteX10 in 20 .. 20){
        print("${noteX10/10},${noteX10%10} | ")
        for(notenZahl in notenZahlen){
            format(0, notenZahl, 0)
        }
        println()
    }
    line()
    for(noteX10 in 21 .. 25){
        val note = noteX10 * 0.1
        print("${noteX10/10},${noteX10%10} | ")
        for(notenZahl in notenZahlen){
            val rest = note + 0.0499 - 2.0
            val dreien = (rest * notenZahl).toInt()
            val zweien = notenZahl - dreien
            format(0, zweien, dreien)
        }
        println()
    }
}*/

fun Int.p() = if(this == 0) " " else ","

fun Int.f2xI(i: Int) = if(this == 0) "      " else "${this.f2()} x $i"
fun Int.f2() = "${if(this>9) (this/10).toString() else " "}${this%10}"

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

fun main(){

    /*val root = */
    FBXReader(
        File(
            OS.downloads,
            "Warrior with animation.fbx"
        ).inputStream()
    )
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