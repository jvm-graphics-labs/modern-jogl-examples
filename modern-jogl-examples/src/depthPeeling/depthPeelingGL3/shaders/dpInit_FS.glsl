//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

vec4 ShadeFragment();

out vec4 outputColor;

void main(void)
{
	vec4 color = ShadeFragment();
	outputColor = vec4(color.rgb * color.a, 1.0 - color.a);
}