#version 330

out vec4 outputColor;

uniform float fragLoopDuration;
uniform float time;

const vec4 firstColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
const vec4 secondColor = vec4(0.0f, 1.0f, 0.0f, 1.0f);

void main()
{
    float currentTime = mod(time, fragLoopDuration);
    float currentLerp = currentTime / fragLoopDuration;

    outputColor = mix(firstColor, secondColor, currentLerp);
}