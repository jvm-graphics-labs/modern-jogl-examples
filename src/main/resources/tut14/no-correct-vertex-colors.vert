#version 330

// Attribute
#define POSITION    0
#define COLOR       1
#define NORMAL      2

layout(location = POSITION) in vec3 position;
layout(location = COLOR) in vec4 color;

noperspective out vec4 theColor;

uniform mat4 cameraToClipMatrix;

void main()
{
    gl_Position = cameraToClipMatrix * vec4(position, 1.0);
    theColor = color;
}
