package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Texture3DBTMaterial
import me.anno.ecs.components.mesh.material.Texture3DBTv2Material
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Texture3D
import me.anno.language.translation.NameDesc
import me.anno.maths.chunks.cartesian.ByteArrayChunkSystem
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.mesh.vox.model.VoxelModel
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGB
import me.anno.utils.structures.maps.Maps.flatten
import org.joml.Vector3f

/**
 * a voxel test world, that can be used for testing
 * */
open class TestWorld : ByteArrayChunkSystem(5, 5, 5, defaultElement = 0) {
    companion object {

        val air = 0.toByte()
        var dirt = 1.toByte()
        var grass = 2.toByte()

        // raytracing currently only supports two colors with my default shader
        var log = 3.toByte()
        var leaves = 4.toByte()
        var water = 5.toByte()
        var lava = 6.toByte()
        var sand = 7.toByte()

        var dirtColor = 0x684530 or black
        var grassColor = 0x2f8d59 or black
        var leafColor = 0x067e3c or (32 shl 24)
        var logColor = 0x463125 or black
        var waterColor = 0x60b6ff or black
        var lavaColor = 0xff931c or black
        var sandColor = 0xdfddae or black

        val colors = mapOf(
            dirt to dirtColor,
            grass to grassColor,
            log to logColor,
            leaves to leafColor,
            water to waterColor,
            lava to lavaColor,
            sand to sandColor,
        )

        val palette = colors
            .mapKeys { it.key.toInt() }
            .flatten(0)

        val blockNames = mapOf(
            dirt to NameDesc("Dirt"),
            grass to NameDesc("Grass"),
            log to NameDesc("Log"),
            leaves to NameDesc("Leaves"),
            water to NameDesc("Water")
        )
    }

    // this world surely could be useful in a few other instances as well 😄
    val treeRandom = FullNoise(1234L)
    val noise = PerlinNoise(1234L, 3, 0.5f, 0f, 1f)

    fun isAir(x: Int, y: Int, z: Int) = getElementAt(x, y, z) == air

    fun canStandOn(x: Int, y: Int, z: Int): Boolean {
        val block = getElementAt(x, y, z)
        return block != air && block != water && block != lava
    }

    fun canStand(x: Int, y: Int, z: Int) = isAir(x, y, z) && isAir(x, y + 1, z) && canStandOn(x, y - 1, z)

    val scale = 0.05f
    val scaleY = scale * 0.5f
    val treeChance = 0.013f

    fun isSolid1(x: Int, y: Int, z: Int): Boolean =
        (y == 0) || noise[x * scale, y * scaleY, z * scale] - y * scaleY > 0.1f

    fun plantTree(chunk: ByteArray, lx: Int, ly: Int, lz: Int) {
        // tree crone
        for (j in -2..2) {
            for (k in -2..2) {
                val sq = j * j + k * k
                if (sq < 8) {
                    chunk[getIndex(lx + j, ly + 3, lz + k)] = leaves
                    chunk[getIndex(lx + j, ly + 4, lz + k)] = leaves
                    if (sq < 4) {
                        chunk[getIndex(lx + j, ly + 5, lz + k)] = leaves
                        if (sq < 2) chunk[getIndex(lx + j, ly + 6, lz + k)] = leaves
                    }
                }
            }
        }
        // stem
        for (i in 0 until 5) chunk[getIndex(lx, ly + i, lz)] = log
    }

    override fun generateChunk(chunkX: Int, chunkY: Int, chunkZ: Int, chunk: ByteArray) {
        val x0 = chunkX shl bitsX
        val y0 = chunkY shl bitsY
        val z0 = chunkZ shl bitsZ
        for (x in x0 until x0 + sizeX) {
            for (z in z0 until z0 + sizeZ) {
                var index = getIndex(x - x0, sizeY - 1, z - z0)
                var aboveIsSolid = isSolid1(x, y0 + sizeY, z)
                for (y in y0 + sizeY - 1 downTo y0) {
                    val isSolid = isSolid1(x, y, z)
                    val block =
                        if (isSolid) {
                            if (aboveIsSolid) dirt
                            else if (y < 13) sand else grass
                        } else if (y < 12) water else air
                    if (block != air) chunk[index] = block
                    aboveIsSolid = isSolid
                    if (block == grass) {
                        // with a chance, place a tree here
                        // this simple method only works distanced from chunk borders
                        if (x - x0 in 2 until sizeX - 2 &&
                            y - y0 in 1 until sizeY - 7 &&
                            z - z0 in 2 until sizeZ - 2 &&
                            treeRandom[x.toFloat(), y.toFloat(), z.toFloat()] < treeChance
                        ) plantTree(chunk, x - x0, y - y0, z - z0)
                    }
                    index -= dy
                }
            }
        }
    }

    fun createRaytracingMesh(x0: Int, y0: Int, z0: Int, sx: Int, sy: Int, sz: Int): MeshComponentBase {
        val texture = Texture3D("blocks", sx, sy, sz)
        texture.createMonochrome { x, y, z -> getElementAt(x0 + x, y0 + y, z0 + z) }
        texture.clamping(Clamping.CLAMP)
        val material = Texture3DBTMaterial()
        dirtColor.toVecRGB(material.color0)
        grassColor.toVecRGB(material.color1)
        material.limitColors(2)
        material.blocks = texture
        return createCube(sx, sy, sz, material)
    }

    fun createRaytracingMeshV2(x0: Int, y0: Int, z0: Int, sx: Int, sy: Int, sz: Int): MeshComponentBase {
        val texture = Texture3D("blocks", sx, sy, sz)
        texture.createRGBA8 { x, y, z ->
            palette[getElementAt(x0 + x, y0 + y, z0 + z)
                .toInt().and(255)]
        }
        val material = Texture3DBTv2Material()
        material.blocks = texture
        return createCube(sx, sy, sz, material)
    }

    private fun createCube(sx: Int, sy: Int, sz: Int, material: Material): MeshComponentBase {
        val mesh = flatCube.scaled(Vector3f(sx, sy, sz)).back
        val comp = MeshComponent()
        comp.materials = listOf(material.ref)
        comp.meshFile = mesh.ref
        return comp
    }

    fun createTriangleMesh1(x0: Int, y0: Int, z0: Int, sx: Int, sy: Int, sz: Int): Mesh {
        val mesh = Mesh()
        object : VoxelModel(sx, sy, sz) {
            override fun getBlock(x: Int, y: Int, z: Int) =
                getElementAt(x0 + x, y0 + y, z0 + z).toInt()
        }.createMesh(palette, null, null, mesh)
        return mesh
    }

    fun createTriangleMeshFromChunk(cx: Int, cy: Int, cz: Int): Mesh {
        val mesh = Mesh()
        val chunk = getChunk(cx, cy, cz, true)!!
        object : VoxelModel(sizeX, sizeY, sizeZ) {
            override fun getBlock(x: Int, y: Int, z: Int) =
                chunk[getIndex(x, y, z)].toInt()
        }.createMesh(palette, null, null, mesh)
        return mesh
    }

    fun createTriangleMesh2(x0: Int, y0: Int, z0: Int, sx: Int, sy: Int, sz: Int): Mesh {
        val model = object : VoxelModel(sx, sy, sz) {
            override fun getBlock(x: Int, y: Int, z: Int) =
                getElementAt(x0 + x, y0 + y, z0 + z).toInt()
        }
        model.center0()
        return model.createMesh(palette, { x, y, z ->
            getElementAt(x0 + x, y0 + y, z0 + z).toInt()
        }, null)
    }

    fun createTriangleMesh(x0: Int, y0: Int, z0: Int, sx: Int, sy: Int, sz: Int): MeshComponent {
        return MeshComponent(createTriangleMesh1(x0, y0, z0, sx, sy, sz))
    }
}
