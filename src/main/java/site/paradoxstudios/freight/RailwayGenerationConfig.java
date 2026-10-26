package site.paradoxstudios.freight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * World topology should remain unchanged once generation begins.
 * Spacing is measured in CHUNKS and must remain constant for a given world.
 */
public final class RailwayGenerationConfig {
    private RailwayGenerationConfig(){}
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue GRID_SPACING;
    public static final ModConfigSpec.IntValue ELEVATION;
    public static final ModConfigSpec.BooleanValue STATIONS;
    public static final ModConfigSpec.BooleanValue TUNNELS;
    public static final ModConfigSpec.BooleanValue BRIDGES;
    public static final ModConfigSpec.IntValue BUILD_BUDGET;

    static {
        var builder=new ModConfigSpec.Builder();
        builder.comment("A deterministic Create track network is generated only in NEW Overworld chunks.",
                "Do not change the spacing or elevation on an established world: neighboring chunks must agree.")
            .push("railway_generation");
        ENABLED=builder.comment("Enable the automatic track network in newly generated chunks.")
            .define("enabled",true);
        GRID_SPACING=builder.comment("Distance, in chunks, between main east-west and north-south lines.",
                "24 chunks = 384 blocks. Stations appear every grid interval.")
            .defineInRange("line_spacing_chunks",24,12,128);
        ELEVATION=builder.comment("Fixed track Y. A fixed Y keeps every chunk seam exactly aligned.",
                "Above ground this becomes a viaduct; below ground it becomes a tunnel.")
            .defineInRange("rail_y",88,68,180);
        STATIONS=builder.comment("Build real Create station blocks and boarding platforms on east-west routes.")
            .define("generate_stations",true);
        TUNNELS=builder.comment("Clear a narrow safety envelope through solid natural terrain.")
            .define("allow_tunnels",true);
        BRIDGES=builder.comment("Build a ballast slab and viaduct piers in open air.")
            .define("allow_bridges",true);
        BUILD_BUDGET=builder.comment("New railway chunks completed per server tick (avoid massive spikes).")
            .defineInRange("max_chunks_per_tick",1,1,4);
        builder.pop();
        SPEC=builder.build();
    }
}
