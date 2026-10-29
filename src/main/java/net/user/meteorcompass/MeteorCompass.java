package net.user.meteorcompass;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MeteorCompass.MOD_ID)
public class MeteorCompass {
    public static final String MOD_ID = "jcraftmeteorcompass";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> METEOR_COMPASS =
            ITEMS.register("meteor_compass", () -> new MeteorCompassItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DEVILS_PALM_COMPASS =
            ITEMS.register("devils_palm_compass", () -> new DevilsPalmCompassItem(new Item.Properties().stacksTo(1)));

    public MeteorCompass() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::addToTab);
    }

    private void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(METEOR_COMPASS);
            event.accept(DEVILS_PALM_COMPASS);
        }
    }
}
