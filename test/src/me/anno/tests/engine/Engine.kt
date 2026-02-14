package me.anno.tests.engine

import me.anno.engine.RemsEngine

// todo handling FileReference is exhausting...
//  maybe create a Ref<Type>?
//  maybe create some structure to handle loading explicitly? some stuff must be available, other things not

// todo our asset pipelines being based on paths is brittle...
//  can we create proper asset workflows, and then use baked assets?

// todo creating a good editor is as important as a good engine
//  we need to set the focus to something, and then just drag things into the scene
//  we can categorize them later

// todo make shaders of materials be references via a file (StaticRef)? this will allow for visual shaders in the future

// todo define custom render modes using files within the project, editable in a GraphEditor

// todo read Nanite paper and find out how we can calculate meshlet hierarchies

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