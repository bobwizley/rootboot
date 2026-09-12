package br.com.bobwizley.rootboot.feature.trimmedarmoredpiglins;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/**
 * Trimmed Armored Piglins: a piglin born wearing armor may have any of its pieces trimmed.
 *
 * <p>Each equipped piece rolls on its own, so zero, one or several pieces of the same piglin can
 * be selected; over a population that reproduces the ~8% outside bastions and ~16% inside them
 * that the reference announces, while keeping the odds readable per piece.
 *
 * <p>The roll happens on the spawn path alone. Loading a chunk never reaches it, which is what
 * keeps an already decided piglin from being rerolled and keeps enabling the feature from
 * touching the piglins a world already has.
 */
public final class TrimmedArmoredPiglins {

    public static final float WILD_CHANCE = 0.25F;
    public static final float BASTION_CHANCE = 0.5F;

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static boolean enabled;

    private TrimmedArmoredPiglins() {
    }

    static void enable() {
        enabled = true;
    }

    static void disable() {
        enabled = false;
    }

    public static void trimOnSpawn(Piglin piglin, ServerLevelAccessor level) {
        if (enabled) {
            trimOnSpawn(
                    piglin,
                    chanceAt(level, piglin.blockPosition()),
                    level.getRandom(),
                    level.registryAccess());
        }
    }

    /**
     * The chance and the random source are parameters so that the game tests can drive both the
     * odds and the pick; the single {@link RandomSource#nextFloat()} per slot below is what makes
     * "one independent roll per equipped piece" a property of the code rather than of a comment.
     */
    static void trimOnSpawn(
            Piglin piglin, float chance, RandomSource random, HolderLookup.Provider registries) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = piglin.getItemBySlot(slot);
            if (piece.isEmpty() || piece.has(DataComponents.TRIM) || random.nextFloat() >= chance) {
                continue;
            }
            List<PiglinTrim> choices = PiglinTrim.choicesFor(slot);
            PiglinTrim trim = choices.get(random.nextInt(choices.size()));
            piece.set(DataComponents.TRIM, trim.resolve(registries));
        }
    }

    static float chanceAt(ServerLevelAccessor level, BlockPos pos) {
        return insideBastion(level, pos) ? BASTION_CHANCE : WILD_CHANCE;
    }

    private static boolean insideBastion(ServerLevelAccessor level, BlockPos pos) {
        // A piglin placed by a bastion piece is born while its chunk is still generating, where
        // only a region-bound manager may read the structure data around it.
        StructureManager structures = level.getLevel().structureManager();
        if (level instanceof WorldGenRegion region) {
            structures = structures.forWorldGenRegion(region);
        }
        return structures
                .getStructureWithPieceAt(
                        pos, structure -> structure.is(BuiltinStructures.BASTION_REMNANT))
                .isValid();
    }
}
