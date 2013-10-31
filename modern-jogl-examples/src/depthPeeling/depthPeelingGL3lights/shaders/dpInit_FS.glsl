//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

smooth in vec4 interpColor;

uniform float alpha;

out vec4 outputColor;

void main(void)
{
    //outputColor.rgb = interpColor.rgb * alpha + vec3(1.0) * (1.0 - alpha);
    //outputColor.a = alpha;

    outputColor = vec4(interpColor.rgb * alpha, alpha);

    //outputColor = interpColor;
}