#version 150

// Vision nocturne : image verte éclaircie, avec du grain et un assombrissement des bords.
uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    l = clamp(pow(l, 0.6) * 1.3 + (noise(texCoord * 731.0) - 0.5) * 0.08, 0.0, 1.0);
    float vignette = smoothstep(0.75, 0.35, distance(texCoord, vec2(0.5)));
    fragColor = vec4(vec3(0.15, 1.0, 0.25) * l * (0.4 + 0.6 * vignette), 1.0);
}
