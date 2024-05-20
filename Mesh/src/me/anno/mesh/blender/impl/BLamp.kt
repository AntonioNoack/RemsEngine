package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_light_types.h
 * */
@Suppress("SpellCheckingInspection")
class BLamp(ptr: ConstructorData) : BlendData(ptr) {

    val r = f32("r")
    val g = f32("g")
    val b = f32("b")

    val type = i32("type")

    val energy = f32("energy")

    val cascadeExponent = f32("cascade_exponent", 4f)
    val cascadeCount = i32("cascade_count", 0)

    val pointRadius = f32("radius")

    val spotRadius = f32("spotsize")

    /**
     *   LA_AREA_SQUARE = 0,
     *   LA_AREA_RECT = 1,
     *   LA_AREA_DISK = 4,
     *   LA_AREA_ELLIPSE = 5,
     * */
    val areaShape = i16("area_shape")
    val areaSizeX = f32("area_size")
    val areaSizeY = f32("area_sizey")
    // val areaSizeZ = float("area_sizez") // unused??

    // {id=ID(152)@0, *adt=AnimData(104)@152, type=short(2)@160, flag=short(2)@162, mode=int(4)@164,
    // r=float(4)@168, g=float(4)@172, b=float(4)@176, k=float(4)@180,
    // shdwr=float(4)@184, shdwg=float(4)@188, shdwb=float(4)@192, shdwpad=float(4)@196,
    // energy=float(4)@200, dist=float(4)@204,
    // spotsize=float(4)@208, spotblend=float(4)@212,
    // att1=float(4)@216, att2=float(4)@220,
    // coeff_const=float(4)@224, coeff_lin=float(4)@228, coeff_quad=float(4)@232,
    // *curfalloff=CurveMapping(392)@240, falloff_type=short(2)@248,
    // clipsta=float(4)@252, clipend=float(4)@256, bias=float(4)@260, soft=float(4)@264,
    // bleedbias=float(4)@268, bleedexp=float(4)@272, bufsize=short(2)@276, samp=short(2)@278, buffers=short(2)@280,
    // filtertype=short(2)@282, bufflag=char(1)@284, buftype=char(1)@285,
    // area_shape=short(2)@286, area_size=float(4)@288, area_sizey=float(4)@292, area_sizez=float(4)@296,
    // sun_angle=float(4)@300,
    // texact=short(2)@308, shadhalostep=short(2)@310, *ipo=Ipo(192)@312, pr_texture=short(2)@320,
    // use_nodes=short(2)@322, *nodetree=bNodeTree(464)@376},
    // cascade_max_dist=float(4)@328, cascade_exponent=float(4)@332, cascade_fade=float(4)@336, cascade_count=int(4)@340,
    // contact_dist=float(4)@344, contact_bias=float(4)@348, contact_spread=float(4)@352, contact_thickness=float(4)@356,
    // spec_fac=float(4)@360, att_dist=float(4)@364, *preview=PreviewImage(64)@368

}