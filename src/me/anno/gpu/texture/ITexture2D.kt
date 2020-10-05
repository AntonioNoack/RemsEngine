package me.anno.gpu.texture

interface ITexture2D {

    var w: Int
    var h: Int


    fun bind(nearest: NearestMode, clampMode: ClampMode)
    fun bind(filtering: FilteringMode, clampMode: ClampMode){
        bind(if(filtering.baseIsNearest) NearestMode.NEAREST else NearestMode.LINEAR, clampMode)
    }

    fun bind(index: Int, nearest: NearestMode, clampMode: ClampMode)
    fun bind(index: Int, filtering: FilteringMode, clampMode: ClampMode){
        bind(index, if(filtering.baseIsNearest) NearestMode.NEAREST else NearestMode.LINEAR, clampMode)
    }

    fun destroy()

}