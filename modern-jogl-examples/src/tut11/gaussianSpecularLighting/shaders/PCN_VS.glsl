#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 inDiffuseColor;
layout(location = 2) in vec3 normal;

out vec4 diffuseColor;
out vec3 vertexNormal;
out vec3 positionCameraSpace;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    vec4 tempCameraPosition = modelToCameraMatrix * vec4(position, 1.0);
    gl_Position = cameraToClipMatrix * tempCameraPosition;

    vertexNormal = normalModelToCameraMatrix * normal;
    diffuseColor = inDiffuseColor;
    positionCameraSpace = vec3(tempCameraPosition);
}