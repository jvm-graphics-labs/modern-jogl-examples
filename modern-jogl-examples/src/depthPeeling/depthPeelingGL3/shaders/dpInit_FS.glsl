//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

out vec4 outputColor;

uniform float Alpha;

void main(void)
{
	vec4 color;
	color.rgb = vec3(.4,.85,.0);

	outputColor = vec4(color.rgb * Alpha, 1.0 - Alpha);
}