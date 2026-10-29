package net.user.meteorcompass;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CompassConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue PILLAR_RINGS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        PILLAR_RINGS = b.comment(
                        "How far the Devil's Pillar Compass searches, in rings of the structure grid.",
                        "Each ring is about 512 blocks (32 chunks) wide, so 20 rings is roughly 10,000 blocks.",
                        "Larger values find farther pillars but can stall the server for several seconds when none are close.")
                .defineInRange("pillarSearchRings", 24, 1, 100);
        SPEC = b.build();
    }

    private CompassConfig() {}
}
