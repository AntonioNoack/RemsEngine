package me.anno.objects.meshes.fbx.structure

import me.anno.io.binary.LittleEndianDataInputStream
import me.anno.objects.meshes.fbx.model.*
import me.anno.utils.OS
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException

// todo create a fbx reader to transform this Video Studio into our own game engine? :)
// todo this data is only a decoded representation -> get the data out of it, including animations <3

class FBXReader(input: InputStream): LittleEndianDataInputStream(input.buffered()),
    FBXNodeBase {

    override val children = ArrayList<FBXNode>()

    val fbxObjects = ArrayList<FBXObject>()
    val fbxObjectMap = HashMap<Long, FBXObject>()
    val root = FBXObject(FBXNode("Root", arrayOf(0L, "Root", "")))

    init {

        readHeader()

        // the end is the empty node
        try {
            while(true){
                children += FBXNode.create(this)
            }
        } catch (e: EmptyNodeException){}

        val out = File(OS.desktop, "fbx.json").outputStream().buffered()
        for(node in children){
            out.write(node.toString().toByteArray())
            out.write('\n'.toInt())
        }
        out.close()

        fbxObjectMap[root.ptr] = root

        children.forEach { nodes ->
            nodes.children.forEach { node ->
                val fbxObject = when(node.nameOrType){
                    "Model" -> FBXModel(node)
                    "Geometry" -> FBXGeometry(node)
                    "Material" -> FBXMaterial(node)
                    "Video" -> FBXVideo(node)
                    "Texture" -> FBXTexture(node)
                    "Pose" -> FBXPose(node)
                    "Deformer" -> FBXDeformer(node)
                    "AnimationStack" -> FBXAnimationStack(node)
                    "AnimationLayer" -> FBXAnimationLayer(node)
                    "NodeAttribute" -> FBXNodeAttribute(node)
                    "AnimationCurve" -> FBXAnimationCurve(node)
                    "AnimationCurveNode" -> FBXAnimationCurveNode(node)
                    else -> null
                }
                if(fbxObject != null){
                    fbxObject.applyProperties(node)
                    fbxObjects += fbxObject
                    fbxObjectMap[fbxObject.ptr] = fbxObject
                }
            }
        }

        children.filter { it.nameOrType == "Connections" }.forEach { connections ->
            connections.children.forEach { node ->
                when(node.nameOrType){
                    "C" -> {
                        // a connection (child, parent)
                        val p = node.properties
                        val type = p[0] as String
                        val n1 = fbxObjectMap[p[1] as Long]
                        val n2 = fbxObjectMap[p[2] as Long]
                        if(n1 == null || n2 == null){
                            // Missing pointers actually occur;
                            // but it's good to check anyways
                            // if(n1 == null) println("Missing ${p[1]}")
                            // if(n2 == null) println("Missing ${p[2]}")
                        } else {
                            when(type){
                                "OO" -> {
                                    // add parent-child relation
                                    assert(p.size == 3)
                                    n2.children.add(n1)
                                }
                                "OP" -> {
                                    // add object override
                                    assert(p.size == 4)
                                    val propertyName = p[3] as String
                                    n1.overrides[propertyName] = n2
                                }
                                else -> throw RuntimeException("Unknown connection type $type")
                            }
                        }
                    }
                }
                Unit
            }
        }

        println(root)

        fbxObjects.filterIsInstance<FBXGeometry>().forEach {
            val realBone = root.children.filterIsInstance<FBXModel>().getOrNull(1)
            if(realBone != null) it.findBoneWeights(realBone)
        }

    }

    fun readHeader(){
        assert(read0String() == "Kaydara FBX Binary  ")
        assert(read() == 0x1a)
        assert(read() == 0x00)
        val version = readInt()
        val major = version / 1000
        val minor = version % 1000
        debug { "Version: $major.$minor" }
    }

    fun debug(msg: () -> String){
        println(msg())
    }

}