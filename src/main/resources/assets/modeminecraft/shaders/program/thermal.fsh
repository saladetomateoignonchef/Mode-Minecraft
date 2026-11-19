#version 150

// Vision thermique : le décor devient froid (bleu sombre), les créatures chaudes (blanc / jaune).
// Les créatures sont entourées d'un contour magenta pur (couleur choisie côté Java car absente du décor) :
// le shader reconnaît cette couleur et la transforme en chaleur.
uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // Décor : dégradé froid (on éclaircit un peu pour que le terrain reste lisible la nuit)
    vec3 cold = mix(vec3(0.03, 0.03, 0.10), vec3(0.32, 0.38, 0.62), clamp(pow(l, 0.7) * 1.1, 0.0, 1.0));
    // Chaleur : pixels magenta (contour des créatures)
    float heat = smoothstep(0.55, 0.85, min(c.r, c.b) - c.g);
    // Le feu et la lave sont aussi chauds
    heat = max(heat, smoothstep(0.75, 0.95, c.r - c.b) * step(0.6, c.g));
    vec3 hot = mix(vec3(1.0, 0.55, 0.15), vec3(1.0, 1.0, 0.9), heat);
    fragColor = vec4(mix(cold, hot, heat), 1.0);
}
