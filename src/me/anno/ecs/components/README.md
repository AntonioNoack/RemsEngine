# Components

Components are the heart of the engine, controlling all logic and data,
where when what should happen, and how things shall be rendered.

Their Unity counterpart is MonoBehaviours, for Unreal it would be BluePrints,
and for Godot, it's scripts.

## Get Started (Custom Components/Behaviours)
- create a new component
- register it using registerCustomClass(MyClass::class), and ensure there is a constructor without any arguments
- add it to your entities in your scene
- now you can start modifying state using onUpdate() and rendering by implementing Renderable()

## Built-In Component Overview

- "anim" contains skeletal animations, including blending and retargeting
- "audio" contains methods to play sounds and music
- "camera" is about what the player can see, and about camera controls (e.g., orbiting)
- "collider" is a collection of 3d shapes that the player or mouse can interact/collide with
- "lights" control the sun, sky, and in-room-lighting and such
- "mesh" is about rendering triangle-based shapes
- to use "physics", you need to add an extra module like Box2D - the folder is just the base classes
- "player" is a partially outdated model on how a player could be defined, regarding eventual multiplayer
- "text" renders letters, words or sentences
- "ui" is a helper to render 2D UI into 3D, like in Cyberpunk 2077