package br.com.bobwizley.rootboot.feature.extraloyaltridents;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExtraLoyalTridentsGameTests {

    private static final int TICKS_TO_CROSS_THE_VOID = 400;

    @GameTest
    public void aLoyalTridentWaitsForAnOwnerThatLeftTheDimension(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);
        ThrownTrident trident = loyalTridentInTheVoid(helper, owner);
        helper.getLevel().getServer().getPlayerList().remove(owner);
        helper.assertTrue(
                trident.getOwner() == null,
                "An owner outside the dimension must be unreachable from the trident");

        tick(trident, TICKS_TO_CROSS_THE_VOID);

        helper.assertFalse(trident.isRemoved(), "The void must not destroy a loyal trident");
        helper.assertTrue(
                ((VoidHeldTrident) trident).rootboot$isHeldInVoid(),
                "A trident waiting in the void must be marked as held");
        helper.assertTrue(
                droppedItems(helper, trident).isEmpty(),
                "Waiting must not hand the trident over as a dropped item");
        trident.discard();
        helper.succeed();
    }

    @GameTest
    public void aLoyalTridentWaitsForADeadOwner(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);
        ThrownTrident trident = loyalTridentInTheVoid(helper, owner);
        owner.setHealth(0.0F);
        helper.assertFalse(owner.isAlive(), "The owner must be dead for this test");

        tick(trident, TICKS_TO_CROSS_THE_VOID);

        helper.assertFalse(trident.isRemoved(), "A dead owner must not cost the trident");
        helper.assertTrue(
                droppedItems(helper, trident).isEmpty(),
                "A dead owner must not turn the trident into a dropped item");
        trident.discard();
        removeOwner(helper, owner);
        helper.succeed();
    }

    @GameTest
    public void theVanillaReturnResumesOnceTheOwnerIsAvailable(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);
        ThrownTrident trident = loyalTridentInTheVoid(helper, owner);
        owner.setHealth(0.0F);
        tick(trident, 20);
        double waitingHeight = trident.getY();

        owner.setHealth(20.0F);
        tick(trident, 40);

        helper.assertTrue(
                trident.getY() > waitingHeight,
                "The vanilla return must lift the trident once its owner is available again");
        helper.assertFalse(
                ((VoidHeldTrident) trident).rootboot$isHeldInVoid(),
                "A trident back inside the world must be left to vanilla");
        trident.discard();
        removeOwner(helper, owner);
        helper.succeed();
    }

    @GameTest
    public void theHoldSurvivesSaveAndLoad(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);
        ThrownTrident savedTrident = loyalTridentInTheVoid(helper, owner);
        helper.getLevel().getServer().getPlayerList().remove(owner);
        tick(savedTrident, 5);

        ThrownTrident trident = tridentInTheVoid(helper, owner, loyalTrident(helper));
        trident.restoreFrom(savedTrident);
        savedTrident.discard();

        helper.assertTrue(
                ((VoidHeldTrident) trident).rootboot$isHeldInVoid(),
                "The hold must survive saving and loading the trident");
        tick(trident, TICKS_TO_CROSS_THE_VOID);
        helper.assertFalse(
                trident.isRemoved(), "A reloaded trident must keep waiting in the void");
        trident.discard();
        helper.succeed();
    }

    @GameTest
    public void aTridentWithoutLoyaltyIsLostToTheVoid(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);

        ThrownTrident trident =
                tridentInTheVoid(helper, owner, Items.TRIDENT.getDefaultInstance());
        tick(trident, TICKS_TO_CROSS_THE_VOID);

        helper.assertTrue(
                trident.isRemoved(), "Only Loyalty earns a trident the protection");
        removeOwner(helper, owner);
        helper.succeed();
    }

    @GameTest
    public void theDisabledFeatureLeavesNewTridentsToVanilla(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        ExtraLoyalTridents.disable();
        try {
            ThrownTrident trident = loyalTridentInTheVoid(helper, owner);

            tick(trident, TICKS_TO_CROSS_THE_VOID);

            helper.assertTrue(
                    trident.isRemoved(),
                    "A disabled feature must leave a trident entering the void to vanilla");
        } finally {
            ExtraLoyalTridents.enable();
        }
        removeOwner(helper, owner);
        helper.succeed();
    }

    @GameTest
    public void theDisabledFeatureKeepsHoldingWhatItAlreadyHolds(GameTestHelper helper) {
        ExtraLoyalTridents.enable();
        ServerPlayer owner = owner(helper);
        ThrownTrident trident = loyalTridentInTheVoid(helper, owner);
        helper.getLevel().getServer().getPlayerList().remove(owner);
        tick(trident, 5);

        ExtraLoyalTridents.disable();
        try {
            tick(trident, TICKS_TO_CROSS_THE_VOID);

            helper.assertFalse(
                    trident.isRemoved(),
                    "Disabling the feature must not drop a trident it is already holding");
        } finally {
            ExtraLoyalTridents.enable();
        }
        trident.discard();
        helper.succeed();
    }

    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 4.0, 1.0));
        owner.setPosRaw(position.x, position.y, position.z);
        return owner;
    }

    private static void removeOwner(GameTestHelper helper, ServerPlayer owner) {
        helper.getLevel().getServer().getPlayerList().remove(owner);
    }

    private static ItemStack loyalTrident(GameTestHelper helper) {
        ItemStack stack = Items.TRIDENT.getDefaultInstance();
        stack.enchant(
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.LOYALTY),
                3);
        return stack;
    }

    private static ThrownTrident loyalTridentInTheVoid(
            GameTestHelper helper, LivingEntity owner) {
        return tridentInTheVoid(helper, owner, loyalTrident(helper));
    }

    private static ThrownTrident tridentInTheVoid(
            GameTestHelper helper, LivingEntity owner, ItemStack stack) {
        ThrownTrident trident = new ThrownTrident(helper.getLevel(), owner, stack);
        trident.pickup = AbstractArrow.Pickup.ALLOWED;
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 4.0, 1.0));
        trident.setPos(position.x, helper.getLevel().getMinY() - 2.0, position.z);
        trident.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(trident);
        return trident;
    }

    private static List<ItemEntity> droppedItems(GameTestHelper helper, ThrownTrident trident) {
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, AABB.ofSize(trident.position(), 64.0, 256.0, 64.0));
    }

    private static void tick(ThrownTrident trident, int count) {
        for (int tick = 0; tick < count && !trident.isRemoved(); tick++) {
            trident.tick();
        }
    }
}
