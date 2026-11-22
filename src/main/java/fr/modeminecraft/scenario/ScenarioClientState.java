package fr.modeminecraft.scenario;

/** État du scénario connu du client (camp, réputation). */
public final class ScenarioClientState {
    public static Side side;
    public static int reputation;
    /** Journal (missions actives, accomplies, privilèges, disponibilités). */
    public static net.minecraft.nbt.CompoundTag missions = new net.minecraft.nbt.CompoundTag();

    private ScenarioClientState() {}
}
