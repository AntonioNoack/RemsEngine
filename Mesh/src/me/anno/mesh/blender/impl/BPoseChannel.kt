package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L236
 * */
@Suppress("SpellCheckingInspection")
class BPoseChannel(ptr: ConstructorData) : BLink<BPoseChannel>(ptr) {

    // *next: bPoseChannel, *prev: bPoseChannel, *prop: IDProperty, constraints: ListBase, name[64]: char,
    // flag: short, ikflag: short, protectflag: short, agrp_index: short, constflag: char, selectflag: char,
    // drawflag: char, bboneflag: char, _pad0[4]: char, *bone: Bone, *parent: bPoseChannel, *child: bPoseChannel,
    // iktree: ListBase, siktree: ListBase, *mpath: bMotionPath, *custom: Object, *custom_tx: bPoseChannel,
    // custom_scale: float, custom_scale_xyz[3]: float, custom_translation[3]: float, custom_rotation_euler[3]: float,
    // loc[3]: float, size[3]: float, eul[3]: float, quat[4]: float,
    // rotAxis[3]: float, rotAngle: float, rotmode: short, _pad[2]: char, chan_mat[4][4]: float,
    // pose_mat[4][4]: float, disp_mat[4][4]: float, disp_tail_mat[4][4]: float,
    // constinv[4][4]: float, pose_head[3]: float, pose_tail[3]: float, limitmin[3]: float, limitmax[3]: float,
    // stiffness[3]: float, ikstretch: float, ikrotweight: float, iklinweight: float, roll1: float, roll2: float,
    // curveInX: float, curveInY: float, curveOutX: float, curveOutY: float, ease1: float, ease2: float,
    // scaleIn: float, scale_in_y: float, scaleOut: float, scale_out_y: float, scale_in[3]: float, scale_out[3]: float,
    // *bbone_prev: bPoseChannel, *bbone_next: bPoseChannel, *temp: void, *draw_data: bPoseChannelDrawData,
    // *orig_pchan: bPoseChannel, runtime: bPoseChannel_Runtime

    val boneName = string("name[64]", 64)
    val bone =
        getPointer("*bone") as? BBone // seems to be set, but I think there isn't really a gurantee, and boneName is safer

    val mat0 = f32s("chan_mat[4][4]", 16) // I think this one is the one we need
    val mat1 = f32s("pose_mat[4][4]", 16)

    override fun toString(): String {
        return "bPoseChannel { '$boneName', [${mat0.joinToString()}], [${mat1.joinToString()}] }"
    }
}