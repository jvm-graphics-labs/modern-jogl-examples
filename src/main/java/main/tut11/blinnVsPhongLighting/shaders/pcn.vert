#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = COLOR) in vec4 diffuseColor;
layout(location = NORMAL) in vec3 normal;

out vec4 diffuseColor_;
out vec3 vertexNormal;
out vec3 cameraSpacePosition;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    vec4 tempCamPosition = (modelToCameraMatrix * vec4(position, 1.0));
    gl_Position = cameraToClipMatrix * tempCamPosition;

    vertexNormal = normalModelToCameraMatrix * normal;
    diffuseColor_ = diffuseColor;
    cameraSpacePosition = vec3(tempCamPosition);
}
