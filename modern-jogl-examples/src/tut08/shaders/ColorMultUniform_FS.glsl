#version 330

smooth in vec4 intermediateColor;

uniform vec4 baseColor;

out vec4 outputColor;

void main()
{
    outputColor = intermediateColor * baseColor;
}