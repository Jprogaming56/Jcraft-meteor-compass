package net.user.meteorcompass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class MeteorCompassItem extends Item {
    private static final String POS = "MeteorPos";
    private static final String DIM = "MeteorDim";

    public MeteorCompassItem(Properties properties) {
        super(properties);
    }

    /** Used by the client needle. */
    @Nullable
    public static GlobalPos getTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(POS) || !tag.contains(DIM)) return null;
        ResourceLocation dim = ResourceLocation.tryParse(tag.getString(DIM));
        if (dim == null) return null;
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLong(POS)));
    }

    private static void setTarget(ItemStack stack, ServerLevel level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(POS, pos.asLong());
        tag.putString(DIM, level.dimension().location().toString());
    }

    private static void clearTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(POS);
            tag.remove(DIM);
        }
    }

    private static int distance(Player player, BlockPos pos) {
        return (int) Math.sqrt(player.blockPosition().distSqr(pos));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            BlockPos found = MeteorFinder.findNearestOre(serverLevel, player.blockPosition());
            if (found == null) {
                clearTarget(stack);
                player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.none"), true);
            } else {
                setTarget(stack, serverLevel, found);
                player.displayClientMessage(
                        Component.translatable("message.jcraftmeteorcompass.found", distance(player, found)), true);
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Every 2 seconds, check the target ore still exists; if it was mined out, retarget. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) return;
        if (level.getGameTime() % 40 != 0) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(POS)) return;
        if (!level.dimension().location().toString().equals(tag.getString(DIM))) return;

        BlockPos pos = BlockPos.of(tag.getLong(POS));
        if (!level.isLoaded(pos)) return;
        if (level.getBlockState(pos).is(MeteorFinder.ore())) return;

        BlockPos next = MeteorFinder.findNearestOre(serverLevel, player.blockPosition());
        if (next == null) {
            clearTarget(stack);
            player.displayClientMessage(Component.translatable("message.jcraftmeteorcompass.depleted"), true);
        } else {
            setTarget(stack, serverLevel, next);
            player.displayClientMessage(
                    Component.translatable("message.jcraftmeteorcompass.retargeted", distance(player, next)), true);
        }
    }
}
