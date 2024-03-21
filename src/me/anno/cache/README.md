# Caching

In a game engine, there are many places of heavy computations that should only be done when necessary,
e.g., loading textures. This folder offers a few base classes for caching things.

## Cache Section

Most times, you'll use a CacheSection, where you have one or two keys, a key-based generator function,
and a timeout value in milliseconds after which a computed value shall be destroyed automatically.

Unless you're coding a tool for desktop, you should always set the async-parameter to true,
so you're not blocking the current thread for the value to be loaded.
On some platforms like web, this synchronous waiting might even throw an exception, because it's impossible to do properly.

The most typical examples in Rem's Engine regarding CacheSection-usage are converting files to meshes, materials, skeletons,
animations, prefabs, textures and similar. The respective classes/objects are called MeshCache, MaterialCache,
SkeletonCache, AnimationCache, PrefabCache and TextureCache. To load CPU-readable images/textures, use ImageCache.

## LastRecentlyUsed-Cache

Sometimes, you might want to use a LastRecentlyUsed-Cache (LRUCache), too.
It has a limited capacity, but offers really fast access to the last recently accessed values.