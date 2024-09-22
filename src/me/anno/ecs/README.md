# Entities - Components - Systems

This engine is both Unity- and ECS-inspired.
The scene structure is like Unity, where Entity = GameObject, Component = MonoBehaviour,
and additionally, there are Systems to operate on all instances of the same type in sequence for improved cache-locality.

## Entities

They define the base scene structure,
and should be used for most things with 3d shapes and behaviours.
(Current exception: SDFComponent)

## Components

Components can add behaviours and rendering to Entities.
Usually, you'll want to implement OnUpdate and override the onUpdate() method,
or use Renderable components like MeshComponent or light components.

## Systems

Systems are singletons, which are called before and on-update.
All entities and components, which are currently part of the main scene and not-disabled, will be (un)registered in each system.