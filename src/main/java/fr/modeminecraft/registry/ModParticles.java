package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particules du mod : douilles éjectées au tir. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ModeMinecraft.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CASING_BRASS =
            PARTICLES.register("casing_brass", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CASING_STEEL =
            PARTICLES.register("casing_steel", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CASING_SHELL =
            PARTICLES.register("casing_shell", () -> new SimpleParticleType(false));

    /** Point lumineux de la traînée des balles traçantes. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACER =
            PARTICLES.register("tracer", () -> new SimpleParticleType(true));

    private ModParticles() {}
}
