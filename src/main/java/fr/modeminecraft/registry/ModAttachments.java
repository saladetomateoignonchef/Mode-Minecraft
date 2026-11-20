package fr.modeminecraft.registry;

import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.item.tactical.TacticalSlots;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Données attachées aux entités. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ModeMinecraft.MODID);

    /** Cases « gilet » et « sac à dos » du joueur (gardées à la mort si la règle keepInventory est active). */
    public static final Supplier<AttachmentType<TacticalSlots>> TACTICAL = ATTACHMENTS.register("tactical_slots",
            () -> AttachmentType.serializable(TacticalSlots::new).copyOnDeath().build());

    private ModAttachments() {}
}
