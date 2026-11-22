package fr.modeminecraft.scenario;

import com.mojang.datafixers.util.Pair;
import fr.modeminecraft.ModeMinecraft;
import fr.modeminecraft.world.city.CityPlanner;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.minecraft.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Carte tactique de toutes les bases (vendue par l'armurier) : position exacte et icône des bases américaines et
 * soviétiques, bases navales, plateformes pétrolières offshore, porte-avions, raffineries du Mesa, cités des milices du
 * désert, métropoles (QG du SWAT) et cartels, sur environ 6 000 blocs autour du joueur.
 */
public class StrategicMapItem extends Item {
    public StrategicMapItem(Properties p) {
        super(p);
    }

    private static final Map<UUID, Pair<Long, ListTag>> CACHE = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST = new HashMap<>();
    private static final java.util.Set<UUID> BUSY = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Rayon couvert par la carte (blocs). */
    private static final int RANGE = 4500;
    private static final String[] VARIANTS = {"base", "soviet", "desert", "refinery", "metropolis", "cartel"};
    private static final String[] STRUCTURES = {"naval_base", "oil_rig", "carrier"};

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            long now = level.getGameTime();
            Pair<Long, ListTag> c = CACHE.get(sp.getUUID());
            BlockPos last = LAST.get(sp.getUUID());
            if (c != null && now - c.getFirst() < 6000 && last != null && last.distManhattan(sp.blockPosition()) < 1500) {
                send(sp, c.getSecond());
            } else if (BUSY.add(sp.getUUID())) {
                sp.displayClientMessage(Component.translatable("message.modeminecraft.map.reading").withStyle(ChatFormatting.GRAY), true);
                UUID id = sp.getUUID();
                var server = sp.getServer();
                sitesAsync(sp.serverLevel(), sp.blockPosition()).whenComplete((sites, err) -> server.execute(() -> {
                    BUSY.remove(id);
                    ServerPlayer again = server.getPlayerList().getPlayer(id);
                    if (sites == null || again == null) {
                        if (err != null) {
                            ModeMinecraft.LOGGER.warn("Carte des bases : {}", err.toString());
                        }
                        return;
                    }
                    CACHE.put(id, Pair.of(server.overworld().getGameTime(), sites));
                    LAST.put(id, again.blockPosition());
                    send(again, sites);
                }));
            }
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    private static void send(ServerPlayer sp, ListTag sites) {
        CompoundTag t = new CompoundTag();
        t.put("Sites", sites);
        PacketDistributor.sendToPlayer(sp, new MapData(t));
    }

    /**
     * Sites autour de `from`, calculés sur un fil d'arrière-plan (calcul pur du générateur : villes du plan déterministe,
     * emplacements des structures maritimes testés comme à la génération), sans bloquer le serveur.
     */
    public static CompletableFuture<ListTag> sitesAsync(ServerLevel level, BlockPos from) {
        var env = CityPlanner.Env.of(level);
        var placement = CityPlanner.placement(level.registryAccess());
        var reg = level.registryAccess();
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        var templates = level.getStructureManager();
        long seed = level.getSeed();
        LevelHeightAccessor height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
        return CompletableFuture.supplyAsync(() -> {
            ListTag out = new ListTag();
            if (placement != null) {
                int rx0 = Math.floorDiv(from.getX() >> 4, placement.spacing()), rz0 = Math.floorDiv(from.getZ() >> 4, placement.spacing());
                int rings = Math.max(2, RANGE / (placement.spacing() * 16));
                for (int i = -rings; i <= rings; i++) {
                    for (int j = -rings; j <= rings; j++) {
                        CityPlanner.City c = CityPlanner.city(env, rx0 + i, rz0 + j);
                        if (c != null && java.util.Arrays.asList(VARIANTS).contains(c.variant())) {
                            out.add(site(c.variant(), c.middleX(), c.middleZ()));
                        }
                    }
                }
            }
            for (String id : STRUCTURES) {
                StructureSet set = reg.registryOrThrow(Registries.STRUCTURE_SET).get(ModeMinecraft.id(id));
                if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement sp)) {
                    continue;
                }
                int sp16 = sp.spacing() * 16;
                int r = RANGE / sp16 + 1;
                int cx0 = Math.floorDiv(from.getX() >> 4, sp.spacing()), cz0 = Math.floorDiv(from.getZ() >> 4, sp.spacing());
                for (int i = -r; i <= r; i++) {
                    for (int j = -r; j <= r; j++) {
                        ChunkPos cp = sp.getPotentialStructureChunk(seed, (cx0 + i) * sp.spacing(), (cz0 + j) * sp.spacing());
                        if (Math.abs(cp.getMiddleBlockX() - from.getX()) > RANGE || Math.abs(cp.getMiddleBlockZ() - from.getZ()) > RANGE) {
                            continue;
                        }
                        for (var entry : set.structures()) {
                            Structure st = entry.structure().value();
                            var ctx = new Structure.GenerationContext(reg, gen, gen.getBiomeSource(), rs, templates, seed, cp, height,
                                    b -> st.biomes().contains(b));
                            var stub = st.findValidGenerationPoint(ctx);
                            if (stub.isPresent()) {
                                out.add(site(id, stub.get().position().getX(), stub.get().position().getZ()));
                                break;
                            }
                        }
                    }
                }
            }
            return out;
        }, Util.backgroundExecutor());
    }

    private static CompoundTag site(String kind, int x, int z) {
        CompoundTag t = new CompoundTag();
        t.putString("K", kind);
        t.putInt("X", x);
        t.putInt("Z", z);
        return t;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modeminecraft.strategic_bases_map").withStyle(ChatFormatting.GRAY));
    }

    public record MapData(CompoundTag data) implements CustomPacketPayload {
        public static final Type<MapData> TYPE = new Type<>(ModeMinecraft.id("strategic_map"));
        public static final StreamCodec<ByteBuf, MapData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(MapData::new, MapData::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MapData d, IPayloadContext ctx) {
            ctx.enqueueWork(() -> fr.modeminecraft.client.BasesMapScreen.open(d.data()));
        }
    }
}
