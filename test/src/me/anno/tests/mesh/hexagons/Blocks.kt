package me.anno.tests.mesh.hexagons

val air: Byte = 0
val stone: Byte = 1
val dirt: Byte = 2
val grass: Byte = 3
val log: Byte = 4
val leaves: Byte = 5
val gravel: Byte = 6
val sand: Byte = 7
val water: Byte = 8

val texIdsXZ = intArrayOf(-1, 1, 2, 3, 20, 52, 17, 18, 223)
val texIdsPY = intArrayOf(-1, 1, 2, 0, 22, 52, 17, 18, 223)
val texIdsNY = intArrayOf(-1, 1, 2, 3, 22, 52, 17, 18, 223)

// categorize blocks into three groups -> shall add wall:
//  - solid (sand)          -> !solid
//  - transparent (water)   -> type != other
//  - cutout (saplings)     -> true
val full = 0
val partial = 1
val fluid = 2
val fluidLow = 3
val matType = intArrayOf(-1, full, full, full, full, partial, full, full, fluidLow)

val fluidLowY = 7f / 8f
