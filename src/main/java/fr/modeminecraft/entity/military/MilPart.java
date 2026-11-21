package fr.modeminecraft.entity.military;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Section avant ou arrière d'un long véhicule (char, camion, hélicoptère) : la boîte de collision d'une entité est
 * carrée, un char de 8 m de long est donc découpé en 3 (avant, centre, arrière). Les tirs touchent toute la longueur
 * et l'impact sait où il a frappé (blindage avant / flancs / arrière, moteur).
 */
public class MilPart extends PartEntity<MilVehicleEntity> {
    public final int index;
    private final EntityDimensions size;

    public MilPart(MilVehicleEntity parent, int index, float width, float height) {
        super(parent);
        this.index = index;
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return getParent().interact(player, hand);
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity e) {
        return this == e || getParent() == e;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
