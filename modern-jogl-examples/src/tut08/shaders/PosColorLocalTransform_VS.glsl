#version 330

layout(location = 0) in vec4 position;
layout(location = 1) in vec4 color;

smooth out vec4 intermediateColor;

uniform mat4 cameraToClipMatrix;
uniform mat4 modelToCameraMatrix;

void main()
{
    vec4 cameraPosition = modelToCameraMatrix * position;
    gl_Position = cameraToClipMatrix * cameraPosition;

    intermediateColor = color;
}