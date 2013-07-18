#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec4 color;

smooth out vec4 intermediateColor;

uniform vec2 offset;

void main()
{
    gl_Position = position + vec4(offset.x, offset.y, 0.0f, 0.0f);
    intermediateColor = color;
}