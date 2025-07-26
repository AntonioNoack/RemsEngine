package me.anno.tests.gfx.shadows

// todo implement trapezoidal shadow maps:
//  given a direction for directional shadow maps, and a view frustum,
//  project the frustum using the direction,
//  then based on camera direction, choose far- and near- side as top/bottom texture/projection side, (rotate the result)
//  then based on the convex hull of all these points, calculate a closely-fit trapezoid shape, around that hull,
//  finally convert this trapezoid plus the shadow/light direction into a 4x4 projection matrix for OpenGL

// Matrix4f.trapezoidCrop
// https://www.comp.nus.edu.sg/~tants/tsm/TSM_recipe.html