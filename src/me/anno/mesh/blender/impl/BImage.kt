package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_image_types.h
 * */
@Suppress("UNCHECKED_CAST", "SpellCheckingInspection")
class BImage(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID
    val name = string("name[1024]", 1024)?.replace('\\', '/') ?: ""

    //val views = inside("views") as BListBase<BImageView>

    // old stuff
    val genX = int("gen_x")
    val genY = int("gen_y")
    //val genZ = short("gen_depth")
    //val genType = byte("gen_type")
    //val genColor = floats("gen_color[4]", 4)

    /**
     * IMA_SRC_FILE = 1,
     * IMA_SRC_SEQUENCE = 2, // used in both cases for my thing...
     * IMA_SRC_MOVIE = 3,
     * IMA_SRC_GENERATED = 4,
     * IMA_SRC_VIEWER = 5, // used for 'Render Result'
     * IMA_SRC_TILED = 6,
     * */
    val source = short("source")

    /**
     * IMA_TYPE_IMAGE = 0,
     * IMA_TYPE_MULTILAYER = 1,
     * /* generated */
     * IMA_TYPE_UV_TEST = 2,
     * /* viewers */
     * IMA_TYPE_R_RESULT = 4,
     * IMA_TYPE_COMPOSITE = 5,
     * */
    val type = short("type")

    val packedFiles = inside("packedfiles") as BListBase<BImagePackedFile>

    /**
     * {id=ID(192)@0, name[1024]=char(1)@192, *cache=MovieCache(0)@1216, *gputexture[3][2]=GPUTexture(0)@1224,
     * anims=ListBase(16)@1272, *rr=RenderResult(0)@1288, renderslots=ListBase(16)@1296, render_slot=short(2)@1312,
     * last_render_slot=short(2)@1314, flag=int(4)@1316, source=short(2)@1320, type=short(2)@1322, lastframe=int(4)@1324,
     * gpuframenr=int(4)@1328, gpuflag=short(2)@1332, gpu_pass=short(2)@1334, gpu_layer=short(2)@1336,
     * gpu_view=short(2)@1338, seam_margin=short(2)@1340, _pad2[2]=char(1)@1342, *packedfile=PackedFile(16)@1344,
     * packedfiles=ListBase(16)@1352, *preview=PreviewImage(64)@1368, lastused=int(4)@1376,
     *
     * generated data
     * gen_x=int(4)@1380, gen_y=int(4)@1384, gen_type=char(1)@1388, gen_flag=char(1)@1389, gen_depth=short(2)@1390,
     * gen_color[4]=float(4)@1392,
     * aspx=float(4)@1408, aspy=float(4)@1412, colorspace_settings=ColorManagedColorspaceSettings(64)@1416,
     * alpha_mode=char(1)@1480, eye=char(1)@1482, views_format=char(1)@1483,
     * offset_x=int(4)@1484, offset_y=int(4)@1488, active_tile_index=int(4)@1492, tiles=ListBase(16)@1496,
     * views=ListBase(16)@1512, *stereo3d_format=Stereo3dFormat(8)@1528, runtime=Image_Runtime(24)@1536}
     * */

    override fun toString(): String {
        return "Image@${position.toString(16)} { $id, $name, $genX x $genY, " +
                "source: $source, type: $type, packed: $packedFiles }"
    }

}