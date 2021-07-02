package me.anno.mesh.jmonkey

import com.jme3.anim.AnimComposer
import com.jme3.anim.SkinningControl
import com.jme3.anim.TransformTrack
import com.jme3.asset.AssetManager
import com.jme3.asset.ModelKey
import com.jme3.scene.*
import com.jme3.scene.control.Control
import com.jme3.system.JmeSystem
import com.jme3.texture.plugins.TGALoader
import me.anno.io.FileReference
import me.anno.mesh.jmonkey.BufferConverter.convertFloats
import me.anno.mesh.jmonkey.BufferConverter.convertInt32
import me.anno.mesh.jmonkey.BufferConverter.convertUByte
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

// todo we're going to use jMonkey, if it supports all animations <3
// todo maybe it even supports TGA completely, then we can remove the ImageIO stuff, which works partially only <3

// todo show bones, generally, for all skeletal animations, in the editor

// todo whether animations using jMonkeyEngine are more reliable than our current solution

object JMonkeyMesh {

    private val LOGGER = LogManager.getLogger(JMonkeyMesh::class)

    var assetManager = createManager()

    fun createManager(): AssetManager {
        val assetCfgUrl = JmeSystem.getPlatformAssetConfigURL()
        val manager = JmeSystem.newAssetManager(assetCfgUrl)
        manager.registerLocator("", CustomLocator::class.java)
        return manager
    }

    fun createModelKey(file: FileReference): ModelKey {
        return ModelKey(file.toString().replace('\\', '/'))
    }

    fun readModel(file: FileReference) {
        val key = createModelKey(file)
        /*val info: AssetInfo = object : AssetInfo(assetManager, key) {
            override fun openStream(): InputStream {
                return file.inputStream().buffered()
            }
        }*/
        val model = assetManager.loadModel(key)
        // val model = FbxLoader().load(info) as Spatial
        val composer = getComponent(model, AnimComposer::class.java)
        if (composer != null) {
            for (clip in composer.animClips) {
                val tracks0 = clip.tracks
                val tracks = tracks0.filterIsInstance<TransformTrack>()
                for(track in tracks){
                    println(track.times?.joinToString())
                    println(track.translations?.joinToString())
                    println(track.rotations?.joinToString())
                    println(track.scales?.joinToString())
                }
                LOGGER.info("Found animation ${clip.name} of length ${clip.length}")
            }
        } else LOGGER.info("Found no animations")
        val skinningControl = getComponent(model, SkinningControl::class.java)
        println(skinningControl)
        if(skinningControl != null){
            val armature = skinningControl.armature
            println(armature)
            // the magic function:
            // println(armature.computeSkinningMatrices())
            // todo we could store animations in two ways:
            //  - skinning matrices (fastest, less flexible (no IK support))
            //  - local transforms (slower, more flexible)
            // inverse kinematics may be possible using transforms and blend spaces only
            println(skinningControl.targets.joinToString())
            println(skinningControl.isHardwareSkinningUsed)
            println(skinningControl.isHardwareSkinningPreferred)
        }
        // todo convert all animations and the hierarchy
        printTree(model, 0)

        model.depthFirstTraversal {
            // println("$it: ${it.localTransform}")
            for(i in 0 until it.numControls){
                println("$it: ${it.getControl(i)}")
            }
        }

    }

   /* fun cloneHierarchy(element: Spatial): Any? {
        when(element){
            is Geometry -> {
                // todo create mesh
                return convertMesh(element)
            }
            is Node -> {
                // todo cameras, lights, ...
                // they are controls, what ever that means...

            }
            else -> return null
        }
    }*/

    fun <V : Control> getComponent(element: Spatial, clazz: Class<V>): V? {
        var composer = element.getControl(clazz)
        if (composer != null) return composer
        if (element is Node) {
            for (child in element.children) {
                composer = getComponent(child, clazz)
                if (composer != null) return composer
            }
        }
        return null
    }

    fun printTree(node: Spatial, depth: Int) {
        when (node) {
            is Node -> {
                for (i in 0 until depth) print("\t")
                println("$node, ${node.localTranslation} ${node.localRotation} ${node.localScale}")
                for (child in node.children) {
                    printTree(child, depth + 1)
                }
            }
            is Geometry -> {
                for (i in 0 until depth + 1) print("\t")
                val mesh = node.mesh
                println("$node, ${node.localTranslation} ${node.localRotation} ${node.localScale}, $mesh, ${mesh.vertexCount} vertices, ${mesh.triangleCount} triangles")
                val convertedMesh = convertMesh(node)
                println(convertedMesh)
            }
            else -> {
                for (i in 0 until depth + 1) print("\t")
                println(node.toString())
            }
        }
    }

    fun convertMesh(geometry: Geometry): me.anno.ecs.components.mesh.Mesh {

        val src = geometry.mesh
        val dst = me.anno.ecs.components.mesh.Mesh()

        val pos = getBuffer(src, VertexBuffer.Type.Position)
        if (pos != null) dst.positions = convertFloats(pos)

        val nor = getBuffer(src, VertexBuffer.Type.Normal)
        if (nor != null) dst.normals = convertFloats(nor)

        val tan = getBuffer(src, VertexBuffer.Type.Tangent)
        if (tan != null) dst.tangents = convertFloats(tan)

        val uvs = getBuffer(src, VertexBuffer.Type.TexCoord) // uv
        if (uvs != null) dst.uvs = convertFloats(uvs)

        val color = getBuffer(src, VertexBuffer.Type.Color) // rgba
        if (color != null) dst.color0 = convertFloats(color)

        val boneWeights = getBuffer(src, VertexBuffer.Type.HWBoneWeight) ?: getBuffer(src, VertexBuffer.Type.BoneWeight)
        val boneIndices = getBuffer(src, VertexBuffer.Type.HWBoneIndex) ?: getBuffer(src, VertexBuffer.Type.BoneIndex)
        if (boneWeights != null && boneIndices != null) {
            dst.boneWeights = convertFloats(boneWeights)
            dst.boneIndices = convertUByte(boneIndices)
        }

        val indices = getBuffer(src, VertexBuffer.Type.Index)
        if (indices != null) dst.indices = convertInt32(indices)

        // todo material indices?

        // todo materials

        val material = geometry.material
        // todo convert the material

        src.morphTargets
        src.numLodLevels

        dst.invalidate()
        return dst

    }

    fun getBuffer(mesh: Mesh, type: VertexBuffer.Type): VertexBuffer? {
        val buffer = mesh.getBuffer(type) ?: return null
        if (buffer.data == null) {
            println("data is null, type: ${buffer.bufferType}")
            return null
        } else println("found values for type: ${buffer.bufferType}, ${buffer.data.capacity()}")
        return buffer
    }

    /*fun convertMaterial(material: Material): me.anno.mesh.assimp.Material {

    }*/

}

fun tgaTest() {
    val file = FileReference("C:\\Users\\Antonio\\Downloads\\ogldev-source\\Content/guard1_body.tga")
    val image = TGALoader.load(file.inputStream(), false)
    val data = image.data
    println(image)
    println(data)
}

fun fbxTest() {
    val file = FileReference(OS.downloads, "azeria/scene.gltf")
    // val file = FileReference(OS.documents, "CuteGhost.fbx")
    JMonkeyMesh.readModel(file)
}

fun main() {
    // tgaTest()
    fbxTest()
}