# Mesh Loader Plugin

This module implements mesh loading for different file formats.
It supports files from Assimp, Blender, .obj, .gltf, Mitsuba Renderer, and Ascii Maya.

It's separated into an extra module, because loading meshes from these file formats is not required for shipped games, and native libraries like Assimp may not be available on every platform.