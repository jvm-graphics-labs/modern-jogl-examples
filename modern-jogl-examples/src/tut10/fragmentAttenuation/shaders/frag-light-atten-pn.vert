#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = NORMAL) in vec3 normal;

out vec4 diffuseColor_;
out vec3 vertexNormal;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;

uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));

    vertexNormal = normalModelToCameraMatrix * normal;
    diffuseColor_ = vec4(1.0);
}
