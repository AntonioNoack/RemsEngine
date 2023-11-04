package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BObject(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    /*
    * id: ID, *adt: AnimData, drawdata: DrawDataList, *sculpt: SculptSession, type: short,
    * partype: short, par1: int, par2: int, par3: int, parsubstr[64]: char, *parent: Object, *track: Object,
    * *proxy: Object, *proxy_group: Object, *proxy_from: Object, *ipo: Ipo, *action: bAction, *poselib: bAction,
    * *pose: bPose, *data: void, *gpd: bGPdata, avs: bAnimVizSettings, *mpath: bMotionPath, constraintChannels: ListBase,
    * effect: ListBase, defbase: ListBase, modifiers: ListBase, greasepencil_modifiers: ListBase, fmaps: ListBase,
    * shader_fx: ListBase, mode: int, restore_mode: int, **mat: Material, *matbits: char, totcol: int, actcol: int,
    * loc[3]: float, dloc[3]: float, size[3]: float, dsize[3]: float, dscale[3]: float, rot[3]: float, drot[3]: float,
    * quat[4]: float, dquat[4]: float, rotAxis[3]: float, drotAxis[3]: float, rotAngle: float, drotAngle: float,
    * obmat[4][4]: float, imat[4][4]: float, parentinv[4][4]: float, constinv[4][4]: float, lay: int, flag: short,
    * colbits: short, transflag: short, protectflag: short, trackflag: short, upflag: short, nlaflag: short,
    * duplicator_visibility_flag: char, base_flag: short, base_local_view_bits: short, col_group: short,
    * col_mask: short, rotmode: short, boundtype: char, collision_boundtype: char, dtx: short, dt: char,
    * empty_drawtype: char, empty_drawsize: float, dupfacesca: float, index: short, actdef: short, actfmap: short,
    * col[4]: float, softflag: short, restrictflag: short, shapenr: short, shapeflag: char, constraints: ListBase,
    * nlastrips: ListBase, hooks: ListBase, particlesystem: ListBase, *pd: PartDeflect, *soft: SoftBody,
    * *dup_group: Collection, *fluidsimSettings: FluidsimSettings, pc_ids: ListBase, *rigidbody_object: RigidBodyOb,
    * *rigidbody_constraint: RigidBodyCon, ima_ofs[2]: float, *iuser: ImageUser, empty_image_visibility_flag: char,
    * empty_image_depth: char, empty_image_flag: char, modifier_flag: uchar, *preview: PreviewImage,
    * lineart: ObjectLineArt, *lightgroup: LightgroupMembership, *lightprobe_cache: LightProbeObjectCache,
    * runtime: Object_Runtime
    */

    val id = inside("id") as BID
    val materials = getStructArray("**mat")
    val type = short("type")
    val parType = short("partype")
    val parent get() = getPointer("*parent") as? BObject

    // Final worldspace matrix with constraints & animsys applied.
    val finalWSMatrix get() = mat4x4("obmat[4][4]")

    val data get() = getPointer("*data") // type specific
    val pose get() = getPointer("*pose") as? BPose // current pose set in armature
    val action get() = getPointer("*action") as? BAction // currently set action (=currently playing animation)

    val modifiers = inside("modifiers") as BListBase<*>

    val animData get() = getPointer("*adt") as? BAnimData

    enum class BObjectType(val id: Int) {

        OB_EMPTY(0),
        OB_MESH(1),
        OB_CURVE(2),
        OB_SURF(3),
        OB_FONT(4),
        OB_MBALL(5),

        OB_LAMP(10),
        OB_CAMERA(11),

        OB_SPEAKER(12),
        OB_LIGHTPROBE(13),

        OB_LATTICE(22),

        OB_ARMATURE(25),

        // Grease Pencil object used in 3D view but not used for annotation in 2D.
        OB_GPENCIL(26),

        OB_HAIR(27),

        OB_POINTCLOUD(28),

        OB_VOLUME(29);
    }

    override fun toString(): String {
        return "BObject { $id, data: $data }"
    }

    companion object {

        val objectTypeById = Array(30) {
            BObjectType.OB_EMPTY
        }

        init {
            for (v in BObjectType.values()) {
                objectTypeById[v.id] = v
            }
        }
    }
}