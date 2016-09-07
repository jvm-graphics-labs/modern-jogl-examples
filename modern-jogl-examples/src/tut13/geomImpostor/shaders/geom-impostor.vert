#version 330

// Attributes
#define CAMERA_SPHERE_POS   6
#define SPHERE_RADIUS       7

layout(location = CAMERA_SPHERE_POS) in vec3 cameraSpherePos;
layout(location = SPHERE_RADIUS) in float sphereRadius;

out VertexData
{
    vec3 cameraSpherePos;
    float sphereRadius;
} vertexData;

void main()
{
    vertexData.cameraSpherePos = cameraSpherePos;
    vertexData.sphereRadius = sphereRadius;
}