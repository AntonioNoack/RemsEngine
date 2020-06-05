package me.anno.objects.effects

// todo post processing layer instead? can save some data shuffling
// todo create a global temporary buffer? to save framebuffers -> works in parallel only; so we need a stack
// todo we need a separate fb for rendering, which then can be disposed
class BlurLayer