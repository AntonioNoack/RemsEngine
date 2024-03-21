# Entities - Components - Systems

ECS is a term, that this engine kind of fulfills, and kind of not.
This engine is very Unity-like.

## Entities

They define the base scene structure,
and should be used for most things with 3d shapes and behaviours.
(Current exception: SDFComponent)

## Components

Components can add behaviours and rendering to Entities.
Usually, you'll want to override the onUpdate() method, or use Renderable components like MeshComponent or light components.

## Systems

Systems aren't really well implemented yet.
If you need a system-like functionality,
use what's there, or build your own system 😅.
Usually, this can be accomplished by having a unique, special component at the scene root.