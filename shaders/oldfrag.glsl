#version 450

in vec2 tc;
out vec4 color;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
layout (binding=1) uniform sampler2D s;

void main(void)
{
	color = texture(s,tc);
}