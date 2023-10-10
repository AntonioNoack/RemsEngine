
-- expanding the sample scene
cubeMesh = OS:getDocuments():getChild('icosphere.obj') -- just a file
cubeMeshI = MeshCache:get(cubeMesh,false) -- mesh instance; API for MeshCache instance might change
dx = cubeMeshI:getBounds():getDeltaX()
dz = cubeMeshI:getBounds():getDeltaZ()
for z=0,5 do
    group = R:Entity('Group ' .. tostring(z), entity)
            group:setPosition(R:Vector3d(0,0,z*dz))
    for x=0,5 do
        child = R:Entity('Instance ' .. tostring(x) .. ', ' .. tostring(z), group)
        child:setPosition(R:Vector3d(x*dx,0,0))
        child:add(R:MeshComponent(cubeMesh))
            end
end

print(cubeMeshI:getBounds())

-- creating and calling Lua from inside Lua xD
-- like JavaScript eval(), highly unsafe xD
q = R:QuickScriptComponent()
q:setCreateScript('print("Hello World!")')
q:onCreate()
