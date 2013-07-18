#version 330

smooth in vec4 intermediateColor;

out vec4 outputColor;

void main()
{
    outputColor = intermediateColor;
    //outputColor = new vec4(0.5f, 0.5f, 0.0f, 1.0f);
}