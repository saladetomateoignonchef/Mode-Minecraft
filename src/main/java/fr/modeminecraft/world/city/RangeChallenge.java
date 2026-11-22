package fr.modeminecraft.world.city;

import fr.modeminecraft.scenario.RangeDrill;
import fr.modeminecraft.scenario.Side;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Stand de tir : cloche de départ du défi chronométré (voir {@link RangeDrill}) et panneau des règles. */
final class RangeChallenge {
    private RangeChallenge() {}

    static void place(B b, Side side) {
        int[] c = RangeDrill.BELL;
        b.set(c[0], c[1] - 1, c[2], Blocks.POLISHED_ANDESITE);
        b.set(c[0], c[1], c[2], Blocks.BELL);
        ScenarioBuildings.sign(b, c[0] + 1, 1, c[2], Direction.SOUTH, side == Side.US
                ? new String[]{"TIMED DRILL", "Ring the bell", "60 s", "15 hits = +rep"}
                : new String[]{"НОРМАТИВ", "Sonnez la cloche", "60 s", "15 cibles = +rép."});
    }
}
