package net.user.meteorcompass;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Devil's Pillar Compass (registry id kept as devils_palm_compass). Points to the nearest
 * JCraft Additions Devil's Pillar that still has Suspicious Corpse Part Sand.
 */
public class DevilsPalmCompassItem extends Item {

    public DevilsPalmCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            BlockPos found = PillarFinder.find(serverLevel, player.blockPosition());
            if (found == null) {
                MeteorCompassItem.clearTarget(stack);
                player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.pillar_none"), true);
            } else {
                MeteorCompassItem.setTarget(stack, serverLevel, found);
                player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.pillar_found",
                        MeteorCompassItem.distance(player, found)), true);
            }
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Every 2 seconds, check the target sand still exists; if it was brushed away, retarget. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) return;
        if (level.getGameTime() % 40 != 0) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("MeteorPos")) return;
        if (!level.dimension().location().toString().equals(tag.getString("MeteorDim"))) return;

        BlockPos pos = BlockPos.of(tag.getLong("MeteorPos"));
        if (!level.isLoaded(pos)) return;
        if (level.getBlockState(pos).is(PillarFinder.target())) return;

        BlockPos next = PillarFinder.find(serverLevel, player.blockPosition());
        if (next == null) {
            MeteorCompassItem.clearTarget(stack);
            player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.pillar_depleted"), true);
        } else {
            MeteorCompassItem.setTarget(stack, serverLevel, next);
            player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.pillar_retargeted",
                    MeteorCompassItem.distance(player, next)), true);
        }
    }
}
