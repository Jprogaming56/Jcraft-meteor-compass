package net.user.meteorcompass;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** Points toward the nearest jcraft:devils_palm biome. Right-click to search. */
public class DevilsPalmCompassItem extends Item {
    private static final ResourceKey<Biome> DEVILS_PALM =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("jcraft", "devils_palm"));

    public DevilsPalmCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Pair<BlockPos, Holder<Biome>> result = serverLevel.findClosestBiome3d(
                    holder -> holder.is(DEVILS_PALM), player.blockPosition(), 6400, 32, 64);
            if (result == null) {
                MeteorCompassItem.clearTarget(stack);
                player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.palm_none"), true);
            } else {
                BlockPos pos = result.getFirst();
                MeteorCompassItem.setTarget(stack, serverLevel, pos);
                player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.palm_found",
                        MeteorCompassItem.distance(player, pos)), true);
            }
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
