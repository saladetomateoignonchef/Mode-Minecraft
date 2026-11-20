package fr.modeminecraft.item.gun;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.EnumSet;
import java.util.List;

/**
 * Armes uniques des Chefs de Faction (butin des boss). Une arme légendaire est l'arme de base marquée du composant
 * LEGENDARY : skin réservé (non modifiable), nom doré et statistiques améliorées.
 */
public enum Legendary implements StringRepresentable {
    /** Glock 18C « Prestige » : culasse dorée, chargeur étendu de 33, rafales ultra-précises. */
    PRESTIGE("prestige", "glock18", GunSkin.PRESTIGE, "boss_swat", 1.2f, 0.7f, 0.7f, 33, false, false, false, 1),
    /** AK-47 « Souverain » : gravures or, crosse en ivoire, recul -30 %, dégâts accrus. */
    SOUVERAIN("souverain", "ak47", GunSkin.SOUVERAIN, "boss_militia", 1.25f, 0.7f, 0.85f, 0, false, false, false, 1),
    /** VSS Vintorez « Fantôme » : aucun bruit de tir, lunette thermique intégrée. */
    FANTOME("fantome", "vss", GunSkin.FANTOME, "boss_garrison", 1.15f, 0.9f, 0.8f, 0, false, true, true, 1),
    /** PKM « Hiver » : bande de 150 balles perforantes, refroidissement amélioré. */
    HIVER("hiver", "pkm", GunSkin.HIVER, "boss_soviet", 1.1f, 0.85f, 0.9f, 150, true, false, false, 0.6f),
    /** Desert Eagle .50 « Cartel » : chromé gravé, tir perforant. */
    CARTEL("cartel", "desert_eagle", GunSkin.CARTEL, "boss_cartel", 1.15f, 0.85f, 0.8f, 0, true, false, false, 1);

    public static final Codec<Legendary> CODEC = StringRepresentable.fromEnum(Legendary::values);
    public static final StreamCodec<ByteBuf, Legendary> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT.map(i -> values()[i], Legendary::ordinal);

    public final String id, gun, boss;
    public final GunSkin skin;
    public final float damageMul, recoilMul, spreadMul, heatMul;
    public final int magazine;
    public final boolean armorPiercing, silent, thermal;

    Legendary(String id, String gun, GunSkin skin, String boss, float damageMul, float recoilMul, float spreadMul, int magazine,
              boolean armorPiercing, boolean silent, boolean thermal, float heatMul) {
        this.id = id;
        this.gun = gun;
        this.skin = skin;
        this.boss = boss;
        this.damageMul = damageMul;
        this.recoilMul = recoilMul;
        this.spreadMul = spreadMul;
        this.magazine = magazine;
        this.armorPiercing = armorPiercing;
        this.silent = silent;
        this.thermal = thermal;
        this.heatMul = heatMul;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Legendary of(ItemStack stack) {
        return stack.get(fr.modeminecraft.registry.ModDataComponents.LEGENDARY.get());
    }

    /** L'arme unique, prête à tirer : chargée, skin réservé, nom doré. */
    public ItemStack create() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("modeminecraft", gun));
        ItemStack s = new ItemStack(item);
        s.set(fr.modeminecraft.registry.ModDataComponents.LEGENDARY.get(), this);
        s.set(fr.modeminecraft.registry.ModDataComponents.SKIN.get(), skin);
        s.set(net.minecraft.core.component.DataComponents.RARITY, Rarity.EPIC);
        s.set(net.minecraft.core.component.DataComponents.ITEM_NAME,
                Component.translatable("legendary.modeminecraft." + id).withStyle(ChatFormatting.GOLD));
        if (item instanceof GunItem g) {
            GunItem.setAmmo(s, magazine > 0 ? magazine : g.stats().magazineSize());
            s.set(fr.modeminecraft.registry.ModDataComponents.CHAMBERED.get(), true);
        }
        return s;
    }

    /** Statistiques de l'arme légendaire. */
    ResolvedGun apply(ResolvedGun r) {
        List<SightMode> modes = thermal ? List.of(new SightMode(0.4f, SightOverlay.THERMAL)) : r.sightModes();
        var specials = r.specials().isEmpty() ? EnumSet.noneOf(AttachmentItem.Special.class) : EnumSet.copyOf(r.specials());
        if (thermal) {
            specials.add(AttachmentItem.Special.THERMAL);
        }
        if (silent) {
            specials.add(AttachmentItem.Special.ECHO);
        }
        return new ResolvedGun(r.damage() * damageMul, magazine > 0 ? Math.max(magazine, r.magazineSize()) : r.magazineSize(),
                r.reloadTicks(), r.spread() * spreadMul, r.aimSpread() * spreadMul, r.recoil() * recoilMul,
                silent ? 0 : r.volume(), r.silenced() || silent, modes, r.movingPenalty(), r.steadyRecoil(), r.steadySpread(),
                r.mobility(), r.aimSpeed(), r.drawMul(), specials);
    }
}
