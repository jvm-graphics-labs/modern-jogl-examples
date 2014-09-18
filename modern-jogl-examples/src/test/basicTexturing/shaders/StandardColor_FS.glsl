#version 330

in vec2 fragmentUV;

out vec4 outputColor;

uniform sampler2D myTexture;

void main()
{
    //outputColor = new vec4(1.0f, 0.0f, 0.0f, 1.0f);

    outputColor = texture(myTexture, fragmentUV).rgba;
}


