package br.com.bobwizley.rootboot.feature.trimmedarmoredpiglins;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

/**
 * A trim a selected armor piece may receive. All the combinations a slot offers carry the same
 * weight, which deliberately replaces the biased sequential rolls of the reference.
 */
record PiglinTrim(ResourceKey<TrimPattern> pattern, ResourceKey<TrimMaterial> material) {

    private static final List<PiglinTrim> ANY_SLOT = List.of(
            new PiglinTrim(TrimPatterns.RIB, TrimMaterials.DIAMOND),
            new PiglinTrim(TrimPatterns.RIB, TrimMaterials.IRON),
            new PiglinTrim(TrimPatterns.RIB, TrimMaterials.NETHERITE),
            new PiglinTrim(TrimPatterns.RIB, TrimMaterials.GOLD),
            new PiglinTrim(TrimPatterns.SNOUT, TrimMaterials.DIAMOND),
            new PiglinTrim(TrimPatterns.SNOUT, TrimMaterials.NETHERITE),
            new PiglinTrim(TrimPatterns.SNOUT, TrimMaterials.GOLD));

    private static final List<PiglinTrim> LEGGINGS = leggings();

    static List<PiglinTrim> choicesFor(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS ? LEGGINGS : ANY_SLOT;
    }

    ArmorTrim resolve(HolderLookup.Provider registries) {
        return new ArmorTrim(
                registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(material),
                registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(pattern));
    }

    private static List<PiglinTrim> leggings() {
        List<PiglinTrim> choices = new ArrayList<>(ANY_SLOT);
        choices.add(new PiglinTrim(TrimPatterns.SILENCE, TrimMaterials.GOLD));
        return List.copyOf(choices);
    }
}
