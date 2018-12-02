#version 450

in vec2 tc;
in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec4 shadowCoordinates;
out vec4 color;

struct PositionalLight
{
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix; // for transforming normals
uniform mat4 shadowMVP2;
layout (binding = 0) uniform sampler2DShadow shTex;
layout (binding = 1) uniform sampler2D s;

void main(void)
{
    // Normalize the light, normal, and view vectors.
    vec3 L = normalize(varyingLightDir);
    vec3 N = normalize(varyingNormal);
    vec3 V = normalize(-varyingVertPos);
    vec3 H = normalize(varyingHalfVector);

    float inShadow = textureProj(shTex, shadowCoordinates);
    //inShadow = 0.0;

    float cosTheta = dot(L, N);
    float cosPhi = dot(H, N);

    vec4 texColor = texture(s, tc);
	color = texColor * (globalAmbient + light.ambient);
	if(inShadow != 0.0)
	{
	    color += (light.diffuse * max(cosTheta, 0.0)) + (light.specular * pow(max(cosPhi, 0.0), 3.0 * 3.0));
	}
}