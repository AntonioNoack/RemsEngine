package me.anno.engine.ui.render

import me.anno.gpu.deferred.DeferredLayerType

enum class RenderMode(val dlt: DeferredLayerType? = null) {

    DEFAULT,
    WITHOUT_POST_PROCESSING,
    CLICK_IDS,
    DEPTH,
    FORCE_DEFERRED,
    FORCE_NON_DEFERRED,
    ALL_DEFERRED_LAYERS,
    ALL_DEFERRED_BUFFERS,

    COLOR(DeferredLayerType.COLOR),
    NORMAL(DeferredLayerType.NORMAL),
    EMISSIVE(DeferredLayerType.EMISSIVE),
    ROUGHNESS(DeferredLayerType.ROUGHNESS),
    METALLIC(DeferredLayerType.METALLIC),
    POSITION(DeferredLayerType.POSITION),
    TRANSLUCENCY(DeferredLayerType.TRANSLUCENCY),
    OCCLUSION(DeferredLayerType.OCCLUSION),
    SHEEN(DeferredLayerType.SHEEN),
    ANISOTROPY(DeferredLayerType.ANISOTROPIC),

    // ALPHA, // currently not defined
    LIGHT_SUM, // todo implement dust-light-spilling for impressive fog
    LIGHT_COUNT,

    SSAO,
    SS_REFLECTIONS,

    INVERSE_DEPTH,
    OVERDRAW, // todo overdraw seems to be missing all cubes... why?
    MSAA_X8,
    WITH_PRE_DRAW_DEPTH,
    MONO_WORLD_SCALE,
    GHOSTING_DEBUG,
    FSR_SQRT2,
    FSR_X2,
    FSR_X4,
    NEAREST_X4,
    LINES, FRONT_BACK,

    SHOW_AABB,
    PHYSICS,

    RAY_TEST,

}