#version 330

// Attribute
#define POSITION    0
#define COLOR       1

layout (location = POSITION) in vec4 position;
layout (location = COLOR) in vec4 color;

smooth out vec4 theColor;

uniform vec2 offset;
uniform mat4 perspectiveMatrix;

void main()
{
    vec4 cameraPosition = position + vec4(offset.x, offset.y, 0.0f, 0.0f);

    gl_Position = perspectiveMatrix * cameraPosition;

    theColor = color;
}