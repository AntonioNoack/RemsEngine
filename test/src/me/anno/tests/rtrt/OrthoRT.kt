package me.anno.tests.rtrt

import me.anno.utils.OS.downloads

// todo city of buildings and stuff
// todo draw using masks, which pixels need to be updated
// todo update only them -> their old and new position updates things, so we draw old + new


// todo idea: the city is very static, so we can raytrace it.
// todo maybe even on Android

// todo represent everything as voxels?
// todo then we can use the tricks, that we learned from different things:
// todo draw the depth at half/quarter resolution first to speed things up massively (only works on first hit though...)
// plus need rougher voxels or LOD

// todo apply all tricks from https://www.youtube.com/watch?v=gsZiJeaMO48 to make it really quick to compute and state of the art :3
// ReSTIR
// C:/Users/Antonio/Documents/Master/global%20illumination/ReSTIR%20GI.pdf
// having watched it:
// simple pdf: <sample on lights without visibility> * 1/dist²
// draw random samples, and heavily reuse them in space and time (with visibility)
// use randomized reservoirs / buckets to save them

// spatial: super reservoirs for large area of effect; 3x3, 9x9, 27x27, ...

// then finally draw sample using this better distribution (with visibility)


// indirect light: sample principle, just that the first pdfs is not a sample to the lights, but completely randomly

// todo -> cyberpunk city simulator? :) hundreds of impressive, in mist, lights
// mist: random chance to spray light near surface according to dust density; exit point according to the density :) -> evaluated on each step-by-step segment (like reservoirs); ray-marching for density

fun main() {
    val sampleScene = downloads.getChild("MagicaVoxel/vox/ForestHeavy.vox")

}