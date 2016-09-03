#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;

uniform mat4 modelToCameraMatrix;

layout(std140) uniform Projection
{
    mat4 cameraToClipMatrix;
};

void main()
{
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1.0));
}