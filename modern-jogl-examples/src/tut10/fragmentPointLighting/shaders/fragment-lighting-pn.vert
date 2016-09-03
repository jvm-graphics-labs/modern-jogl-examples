#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = NORMAL) in vec3 normal;

out vec4 diffuseColor_;
out vec3 vertexNormal;
out vec3 modelSpacePosition;

uniform mat4 modelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));

    vertexNormal = normal;
    modelSpacePosition = position;
    diffuseColor_ = vec4(1.0);
}
