
#version 330

#include semantic.glsl


layout(location = POSITION) in vec3 position;
layout(location = COLOR) in vec4 diffuseColor;
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

    modelSpacePosition = position;
    vertexNormal = normal;
    diffuseColor_ = diffuseColor;
}
