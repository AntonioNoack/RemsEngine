package me.anno.tests.engine

import me.anno.engine.RemsEngine

// todo animation samples:
//  - 2d (frames) jump & run
//  - 3d (skeletal) shooter

// todo when clicking on prefab file (material) input, we need the following options:
//  - create temporary file (testing only)
//  - create file stored in project
//  - create prefab based on that in project
//  - create temporary file based on that

/**
 * This start-the-engine function is located in the test project,
 * because here all extensions are available, while the main project turns into a "base" project.
 * */
fun main() {
    RemsEngine().run()
}