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

    /** Flash de bouche (étoile lumineuse au bout du canon). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MUZZLE_FLASH =
            PARTICLES.register("muzzle_flash", () -> new SimpleParticleType(true));

    /** Laser : point (alpha 255) ou faisceau (alpha plus faible), de la couleur donnée. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> LASER =
            PARTICLES.register("laser", () -> new ParticleType<net.minecraft.core.particles.ColorParticleOption>(true) {
                @Override
                public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec() {
                    return net.minecraft.core.particles.ColorParticleOption.codec(this);
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,
                        net.minecraft.core.particles.ColorParticleOption> streamCodec() {
                    return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);
                }
            });

    private ModParticles() {}
}
