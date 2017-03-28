#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = NORMAL) in vec3 normal;

out vec3 vertexNormal;
out vec3 cameraSpacePosition;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

void main()
{
    vec4 tempCamPosition = (modelToCameraMatrix * vec4(position, 1.0));
    gl_Position = cameraToClipMatrix * tempCamPosition;

    vertexNormal = normalize(normalModelToCameraMatrix * normal);
    cameraSpacePosition = vec3(tempCamPosition);
}
