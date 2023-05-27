# Path Finding / NavMesh

These samples show how multiple agents can search paths at the same time.

## [Navigation Mesh: With Raycasts](NavMeshRaycast.kt)

This sample uses raycasting to make sure the agents are truly on their correct y-value.

The main bottleneck was raycasting for 250 agents at every single frame. Now that I do it only every  16th frame,
it's much smoother for me 😊 (running at 60 fps with 500 agents).

## [Navigation Mesh: A few](NavMeshSmall.kt)

Five agents walking freely.

## [Navigation Mesh: Just many agents](NavMeshMany.kt)

This sample drops raycasting in favour of having more agents.
Currently still limited, because world is quite small and crowded.