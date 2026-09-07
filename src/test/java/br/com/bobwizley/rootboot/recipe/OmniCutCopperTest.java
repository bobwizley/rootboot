package br.com.bobwizley.rootboot.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bobwizley.rootboot.recipe.RecipeSpec.Ingredient;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Shaped;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Stonecutting;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Exercises OmniCut's copper slice: the eight weathering states, the five products vanilla's stonecutter
 * does not offer, and the recovery direction back into cut copper.
 */
class OmniCutCopperTest {

    private static final List<String> WEATHERING =
            List.of(
                    "",
                    "exposed_",
                    "weathered_",
                    "oxidized_",
                    "waxed_",
                    "waxed_exposed_",
                    "waxed_weathered_",
                    "waxed_oxidized_");

    /** The block each state cuts from, spelled out because vanilla's ids do not follow the prefix. */
    private static final Map<String, String> SOURCE_BLOCK =
            Map.of(
                    "", "copper_block",
                    "exposed_", "exposed_copper",
                    "weathered_", "weathered_copper",
                    "oxidized_", "oxidized_copper",
                    "waxed_", "waxed_copper_block",
                    "waxed_exposed_", "waxed_exposed_copper",
                    "waxed_weathered_", "waxed_weathered_copper",
                    "waxed_oxidized_", "waxed_oxidized_copper");

    /** The reference's "vanilla price" yields, verbatim. */
    private static final Map<String, Integer> CUT_YIELD =
            Map.of(
                    "copper_bars", 24,
                    "copper_chain", 8,
                    "lightning_rod", 3,
                    "copper_door", 5,
                    "copper_trapdoor", 4);

    /** What vanilla's own stonecutter already produces from a copper block; ours must not repeat it. */
    private static final List<String> VANILLA_CUTS =
            List.of("cut_copper", "cut_copper_stairs", "cut_copper_slab", "chiseled_copper", "copper_grate");

    private RecipeSpec byPath(String path) {
        return OmniCutCopper.all().stream()
                .filter(spec -> spec.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing recipe rootboot:" + path));
    }

    @Test
    void coversEveryStateOncePerConversionAndNothingElse() {
        List<String> expected =
                WEATHERING.stream()
                        .flatMap(
                                weathering ->
                                        Stream.concat(
                                                CUT_YIELD.keySet().stream()
                                                        .map(product -> "copper/cut/" + weathering + product),
                                                Stream.of(
                                                        "copper/uncut/" + weathering + "cut_copper",
                                                        "copper/unslab/" + weathering + "cut_copper")))
                        .toList();

        List<String> paths = OmniCutCopper.all().stream().map(RecipeSpec::path).toList();
        assertEquals(Set.copyOf(expected), Set.copyOf(paths));
        assertEquals(expected.size(), paths.size(), "no conversion may be emitted twice");
        assertTrue(
                OmniCutCopper.all().stream().allMatch(spec -> spec.namespace().equals("rootboot")),
                "the whole slice is additive, so nothing may claim a minecraft id");
    }

    @Test
    void everyStateCutsTheFiveProductsVanillaDoesNotOffer() {
        for (String weathering : WEATHERING) {
            for (Map.Entry<String, Integer> product : CUT_YIELD.entrySet()) {
                String id = weathering + product.getKey();
                Stonecutting recipe =
                        assertInstanceOf(Stonecutting.class, byPath("copper/cut/" + id), id);

                assertEquals(
                        List.of(Ingredient.item("minecraft:" + SOURCE_BLOCK.get(weathering))),
                        recipe.ingredients(),
                        () -> "input of " + id);
                assertEquals("minecraft:" + id, recipe.result().item());
                assertEquals(product.getValue(), recipe.result().count(), () -> "yield of " + id);
            }
        }
    }

    // Vanilla cuts the cut-copper family itself, so repeating any of it here would be a duplicate.
    @Test
    void nothingCutsWhatVanillaAlreadyCuts() {
        List<String> repeated =
                OmniCutCopper.all().stream()
                        .filter(spec -> spec.path().startsWith("copper/cut/"))
                        .map(spec -> spec.result().item())
                        .filter(
                                item ->
                                        VANILLA_CUTS.stream()
                                                .anyMatch(vanilla -> item.endsWith(vanilla)))
                        .toList();

        assertEquals(List.of(), repeated, "the cut copper family belongs to vanilla");
    }

    // The trapdoor is cut at nearly twice its bench price, so letting one buy material back would multiply
    // copper without limit. This guards the copper slice only; a mod-wide guard would have to cost every
    // item, since a recipe consuming another's output is normal and not by itself a multiplier.
    @Test
    void noProductCutHereIsAlsoRecoverable() {
        Set<String> cut =
                OmniCutCopper.all().stream()
                        .filter(spec -> spec.path().startsWith("copper/cut/"))
                        .map(spec -> spec.result().item())
                        .collect(java.util.stream.Collectors.toSet());

        List<String> recoverable =
                OmniCutCopper.all().stream()
                        .filter(spec -> !spec.path().startsWith("copper/cut/"))
                        .flatMap(spec -> inputs(spec).stream())
                        .filter(cut::contains)
                        .toList();

        assertEquals(List.of(), recoverable, "no recovery may consume a product this slice cuts");
    }

    @Test
    void everyStateRecyclesStairsChiseledAndGrateBackIntoOneCutCopper() {
        for (String weathering : WEATHERING) {
            String block = weathering + "cut_copper";
            Stonecutting recipe =
                    assertInstanceOf(Stonecutting.class, byPath("copper/uncut/" + block), block);

            assertEquals(
                    List.of(
                            Ingredient.item("minecraft:" + weathering + "cut_copper_stairs"),
                            Ingredient.item("minecraft:" + weathering + "chiseled_copper"),
                            Ingredient.item("minecraft:" + weathering + "copper_grate")),
                    recipe.ingredients(),
                    () -> "inputs of " + block);
            assertEquals("minecraft:" + block, recipe.result().item());
            assertEquals(1, recipe.result().count());
        }
    }

    @Test
    void everyStateReassemblesTwoSlabsIntoOneCutCopper() {
        for (String weathering : WEATHERING) {
            String block = weathering + "cut_copper";
            Shaped recipe = assertInstanceOf(Shaped.class, byPath("copper/unslab/" + block), block);

            assertEquals(List.of("##"), recipe.pattern(), "side by side, never stacked");
            assertEquals(Ingredient.item("minecraft:" + block + "_slab"), recipe.key().get('#'));
            assertEquals("minecraft:" + block, recipe.result().item());
            assertEquals(1, recipe.result().count());
        }
    }

    @Test
    void recoveryStillOnlyEverGivesBackCutCopper() {
        assertTrue(
                OmniCutCopper.all().stream()
                        .filter(spec -> !spec.path().startsWith("copper/cut/"))
                        .allMatch(spec -> spec.result().item().endsWith("cut_copper")),
                "every recovery recipe rebuilds a cut copper block");
        assertFalse(
                OmniCutCopper.all().stream()
                        .anyMatch(spec -> spec.result().count() != 1 && !spec.path().startsWith("copper/cut/")),
                "recovery is one unit, the vanilla price");
    }

    private static List<String> inputs(RecipeSpec spec) {
        return switch (spec) {
            case Stonecutting stonecutting ->
                    stonecutting.ingredients().stream().map(Ingredient::id).toList();
            case Shaped shaped -> shaped.key().values().stream().map(Ingredient::id).toList();
            case RecipeSpec.Shapeless shapeless ->
                    shapeless.ingredients().stream().map(Ingredient::id).toList();
            case RecipeSpec.Cooking cooking -> cooking.inputs();
        };
    }
}
