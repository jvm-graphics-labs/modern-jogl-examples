
#version 330

#include semantic.glsl


layout(location = POSITION) in vec3 position;
layout(location = NORMAL) in vec3 normal;

out vec4 diffuseColor_;
out vec3 vertexNormal;
out vec3 cameraSpacePosition;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

uniform vec4 baseDiffuseColor;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    vec4 tempCamPosition = (modelToCameraMatrix * vec4(position, 1.0));
    gl_Position = cameraToClipMatrix * tempCamPosition;

    vertexNormal = normalModelToCameraMatrix * normal;
    diffuseColor_ = baseDiffuseColor;
    cameraSpacePosition = vec3(tempCamPosition);
}
