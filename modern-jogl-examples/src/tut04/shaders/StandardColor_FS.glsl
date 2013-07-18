#version 330

smooth in vec4 intermediateColor;

out vec4 outputColor;

void main()
{
    outputColor = intermediateColor;
}