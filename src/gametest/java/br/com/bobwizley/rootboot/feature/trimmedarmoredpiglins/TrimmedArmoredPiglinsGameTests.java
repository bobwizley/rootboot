package br.com.bobwizley.rootboot.feature.trimmedarmoredpiglins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class TrimmedArmoredPiglinsGameTests {

    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    /** Fixed so that every rate and tally below is the same draw on every run. */
    private static final long SEED = 26_02L;

    private static final Map<EquipmentSlot, Item> GOLDEN_ARMOR = Map.of(
            EquipmentSlot.HEAD, Items.GOLDEN_HELMET,
            EquipmentSlot.CHEST, Items.GOLDEN_CHESTPLATE,
            EquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS,
            EquipmentSlot.FEET, Items.GOLDEN_BOOTS);

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final PiglinTrim SILENT_GOLD =
            new PiglinTrim(TrimPatterns.SILENCE, TrimMaterials.GOLD);

    private static final int ROUNDS = 4000;
    private static final float RATE_TOLERANCE = 0.02F;

    @GameTest
    public void piglinBrutesAreIneligible(GameTestHelper helper) {
        TrimmedArmoredPiglins.enable();
        PiglinBrute brute = helper.spawn(EntityTypes.PIGLIN_BRUTE, SPAWN);
        wearGoldenArmor(brute);

        ServerLevel level = helper.getLevel();
        brute.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(brute.blockPosition()),
                EntitySpawnReason.NATURAL,
                null);

        helper.assertValueEqual(trimCount(brute), 0, "trimmed pieces on a piglin brute");
        helper.succeed();
    }

    @GameTest
    public void piglinsWithoutArmorAreIneligible(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            piglin.setItemSlot(slot, ItemStack.EMPTY);
        }

        TrimmedArmoredPiglins.trimOnSpawn(
                piglin, 1.0F, RandomSource.create(SEED), helper.getLevel().registryAccess());

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            helper.assertTrue(
                    piglin.getItemBySlot(slot).isEmpty(),
                    "An empty " + slot.getName() + " slot must stay empty");
        }
        helper.succeed();
    }

    @GameTest
    public void eachEquippedPieceRollsOnItsOwn(GameTestHelper helper) {
        assertTrimRate(helper, TrimmedArmoredPiglins.WILD_CHANCE);
        assertTrimRate(helper, TrimmedArmoredPiglins.BASTION_CHANCE);
        helper.succeed();
    }

    @GameTest
    public void aPiglinAwayFromABastionRollsAtTheWildChance(GameTestHelper helper) {
        helper.assertValueEqual(
                TrimmedArmoredPiglins.chanceAt(helper.getLevel(), helper.absolutePos(SPAWN)),
                TrimmedArmoredPiglins.WILD_CHANCE,
                "chance outside a bastion");
        helper.succeed();
    }

    @GameTest
    public void aPiglinInsideABastionRollsAtTheBastionChance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(SPAWN);
        Structure bastion = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(BuiltinStructures.BASTION_REMNANT)
                .value();
        ChunkAccess chunk = level.getChunk(pos);
        try {
            chunk.setStartForStructure(bastion, coveringStart(bastion, chunk, pos));
            chunk.addReferenceForStructure(bastion, chunk.getPos().pack());

            helper.assertValueEqual(
                    TrimmedArmoredPiglins.chanceAt(level, pos),
                    TrimmedArmoredPiglins.BASTION_CHANCE,
                    "chance inside a bastion");
        } finally {
            chunk.setStartForStructure(bastion, StructureStart.INVALID_START);
        }
        helper.succeed();
    }

    /**
     * A start whose single piece covers {@code pos}, which is all the structure lookup reads.
     * Generating a real bastion would cost far more than the one bounding box under test.
     */
    private static StructureStart coveringStart(
            Structure structure, ChunkAccess chunk, BlockPos pos) {
        return new StructureStart(
                structure,
                chunk.getPos(),
                0,
                new PiecesContainer(List.of(new BuriedTreasurePieces.BuriedTreasurePiece(pos))));
    }

    @GameTest
    public void selectedPiecesChooseUniformlyAmongTheDefinedCombinations(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);
        RandomSource random = RandomSource.create(SEED);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        Map<EquipmentSlot, Map<PiglinTrim, Integer>> tally = new HashMap<>();
        for (int round = 0; round < ROUNDS; round++) {
            wearGoldenArmor(piglin);
            TrimmedArmoredPiglins.trimOnSpawn(piglin, 1.0F, random, registries);
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                tally.computeIfAbsent(slot, unused -> new HashMap<>())
                        .merge(trimOf(piglin, slot), 1, Integer::sum);
            }
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            List<PiglinTrim> expected = PiglinTrim.choicesFor(slot);
            Map<PiglinTrim, Integer> drawn = tally.get(slot);
            helper.assertValueEqual(
                    drawn.keySet(), Set.copyOf(expected), "combinations drawn for " + slot.getName());
            for (PiglinTrim trim : expected) {
                assertClose(
                        helper,
                        drawn.get(trim) / (float) ROUNDS,
                        1.0F / expected.size(),
                        "share of " + trim + " on " + slot.getName());
            }
        }
        helper.assertTrue(
                tally.get(EquipmentSlot.LEGS).containsKey(SILENT_GOLD),
                "Leggings must be able to draw Silence with gold");
        helper.assertFalse(
                tally.get(EquipmentSlot.HEAD).containsKey(SILENT_GOLD),
                "Only leggings may draw Silence with gold");
        helper.succeed();
    }

    @GameTest
    public void anAlreadyDecidedPiglinIsNeverRerolled(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);
        wearGoldenArmor(piglin);
        TrimmedArmoredPiglins.trimOnSpawn(
                piglin, 1.0F, RandomSource.create(SEED), helper.getLevel().registryAccess());
        List<ArmorTrim> decided = trims(piglin);

        LivingEntity loaded = reload(helper, piglin);

        helper.assertValueEqual(trims(loaded), decided, "trims after a reload");
        helper.succeed();
    }

    @GameTest
    public void anUntrimmedPiglinStaysUntrimmedWhenItLoads(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);
        wearGoldenArmor(piglin);

        LivingEntity loaded = reload(helper, piglin);

        helper.assertValueEqual(trimCount(loaded), 0, "trimmed pieces after a reload");
        helper.succeed();
    }

    @GameTest
    public void theToggleDecidesWhetherNewbornsAreProcessed(GameTestHelper helper) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);

        TrimmedArmoredPiglins.disable();
        helper.assertValueEqual(
                spawnedTrimCount(helper, piglin), 0, "trimmed pieces while disabled");

        TrimmedArmoredPiglins.enable();
        helper.assertTrue(
                spawnedTrimCount(helper, piglin) > 0,
                "An enabled feature must trim armor on the spawn path");
        helper.succeed();
    }

    /**
     * Drives the real spawn path so that the hook itself, and not only the logic behind it, is
     * what decides the armor.
     */
    private static int spawnedTrimCount(GameTestHelper helper, Piglin piglin) {
        ServerLevel level = helper.getLevel();
        int trimmed = 0;
        for (int round = 0; round < 60; round++) {
            wearGoldenArmor(piglin);
            piglin.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(piglin.blockPosition()),
                    EntitySpawnReason.NATURAL,
                    null);
            trimmed += trimCount(piglin);
        }
        return trimmed;
    }

    private static void assertTrimRate(GameTestHelper helper, float chance) {
        Piglin piglin = helper.spawn(EntityTypes.PIGLIN, SPAWN);
        RandomSource random = RandomSource.create(SEED);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        int trimmed = 0;
        int untouchedPiglins = 0;
        int fullyTrimmedPiglins = 0;
        for (int round = 0; round < ROUNDS; round++) {
            wearGoldenArmor(piglin);
            TrimmedArmoredPiglins.trimOnSpawn(piglin, chance, random, registries);
            int count = trimCount(piglin);
            trimmed += count;
            if (count == 0) {
                untouchedPiglins++;
            } else if (count == ARMOR_SLOTS.size()) {
                fullyTrimmedPiglins++;
            }
        }
        piglin.discard();

        assertClose(
                helper,
                trimmed / (float) (ROUNDS * ARMOR_SLOTS.size()),
                chance,
                "share of pieces trimmed at a chance of " + chance);
        helper.assertTrue(
                untouchedPiglins > 0 && fullyTrimmedPiglins > 0,
                "Independent rolls must produce both untouched and fully trimmed piglins");
    }

    private static void wearGoldenArmor(LivingEntity entity) {
        GOLDEN_ARMOR.forEach((slot, item) -> entity.setItemSlot(slot, new ItemStack(item)));
    }

    private static int trimCount(LivingEntity entity) {
        return (int) ARMOR_SLOTS.stream()
                .filter(slot -> entity.getItemBySlot(slot).has(DataComponents.TRIM))
                .count();
    }

    private static List<ArmorTrim> trims(LivingEntity entity) {
        List<ArmorTrim> found = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            found.add(entity.getItemBySlot(slot).get(DataComponents.TRIM));
        }
        return found;
    }

    private static PiglinTrim trimOf(LivingEntity entity, EquipmentSlot slot) {
        ArmorTrim trim = entity.getItemBySlot(slot).get(DataComponents.TRIM);
        return new PiglinTrim(
                trim.pattern().unwrapKey().orElseThrow(), trim.material().unwrapKey().orElseThrow());
    }

    /**
     * Round-trips the entity through its save data and back into the level, the way unloading and
     * loading a chunk does.
     */
    private static LivingEntity reload(GameTestHelper helper, LivingEntity entity) {
        ServerLevel level = helper.getLevel();
        TagValueOutput saved =
                TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.save(saved);
        entity.discard();

        Entity loaded = EntityType.loadEntityRecursive(
                TagValueInput.create(
                        ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()),
                level,
                EntitySpawnReason.LOAD,
                EntityProcessor.NOP);
        if (!(loaded instanceof LivingEntity livingEntity)) {
            helper.fail(Component.literal("The entity could not be reloaded from its save data"));
            throw new IllegalStateException();
        }
        if (!level.addFreshEntity(livingEntity)) {
            helper.fail(Component.literal("The reloaded entity could not re-enter the level"));
        }
        return livingEntity;
    }

    private static void assertClose(
            GameTestHelper helper, float actual, float expected, String description) {
        if (Math.abs(actual - expected) > RATE_TOLERANCE) {
            helper.fail(Component.literal(
                    description + " (expected about " + expected + ", found " + actual + ")"));
        }
    }
}
