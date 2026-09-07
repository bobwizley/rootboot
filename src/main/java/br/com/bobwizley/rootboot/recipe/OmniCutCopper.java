package br.com.bobwizley.rootboot.recipe;

import br.com.bobwizley.rootboot.recipe.RecipeSpec.Category;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Ingredient;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Result;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Shaped;
import br.com.bobwizley.rootboot.recipe.RecipeSpec.Stonecutting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OmniCut's copper slice: the stonecutter cuts a copper block into the five products vanilla's own
 * stonecutter has no recipe for, cuts stairs, a chiseled block or a grate back down into the cut copper
 * they came from, and the crafting table puts two slabs back together.
 *
 * <p>The rest of the forward direction is left to vanilla, which already cuts {@code cut_copper}, its
 * stairs, its slab, the chiseled block and the grate. Bars, chain, lightning rod, door and trapdoor are the
 * gap the reference fills and vanilla does not (docs/FEATURES.md).
 *
 * <p>Recovery gives back one unit, the reference's "vanilla price" figure: each of these products costs one
 * cut copper in the stonecutter, and a slab costs half of one, so a pair of them rebuilds a whole.
 *
 * <p>"Vanilla price" names the reference's calibration, not an exact cost match. Measured against what the
 * crafting table charges, bars and lightning rods come out even, chain and door are slightly cheap, and the
 * trapdoor is far cheaper: four of them cost sixteen ingots on the bench and one block here. That is a
 * deliberate discount, not a loop — no product cut here is recoverable anywhere in RootBoot, so none of it
 * can be turned back into material.
 */
final class OmniCutCopper {

    /**
     * The eight weathering states, as the prefix vanilla puts on every id in the family. The pristine state
     * has no prefix at all, which is why these are prefixes rather than names.
     */
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

    private static final String WAXED = "waxed_";

    /** What the stonecutter cuts a copper block into, at the reference's yields. */
    private static final List<Product> CUT_PRODUCTS =
            List.of(
                    new Product("copper_bars", 24, Category.DECORATIONS),
                    new Product("copper_chain", 8, Category.DECORATIONS),
                    new Product("lightning_rod", 3, Category.REDSTONE),
                    new Product("copper_door", 5, Category.REDSTONE),
                    new Product("copper_trapdoor", 4, Category.REDSTONE));

    /** What recycles into cut copper. The slab is absent: it goes through the crafting table instead. */
    private static final List<String> RECYCLED_PRODUCTS =
            List.of("cut_copper_stairs", "chiseled_copper", "copper_grate");

    private OmniCutCopper() {
    }

    static List<RecipeSpec> all() {
        List<RecipeSpec> recipes = new ArrayList<>();
        for (String weathering : WEATHERING) {
            for (Product product : CUT_PRODUCTS) {
                recipes.add(cut(weathering, product));
            }
            recipes.add(uncut(weathering));
            recipes.add(unslab(weathering));
        }
        return List.copyOf(recipes);
    }

    private static RecipeSpec cut(String weathering, Product product) {
        String result = weathering + product.name();
        return new Stonecutting(
                "rootboot",
                "copper/cut/" + result,
                product.category(),
                Optional.empty(),
                List.of(Ingredient.item(item(weatheredBlock(weathering)))),
                new Result(item(result), product.count()));
    }

    private static RecipeSpec uncut(String weathering) {
        String block = weathering + "cut_copper";
        return new Stonecutting(
                "rootboot",
                "copper/uncut/" + block,
                Category.BUILDING_BLOCKS,
                Optional.empty(),
                RECYCLED_PRODUCTS.stream()
                        .map(product -> Ingredient.item(item(weathering + product)))
                        .toList(),
                new Result(item(block), 1));
    }

    // Side by side rather than stacked, matching the wood and rock slices and the reference.
    private static RecipeSpec unslab(String weathering) {
        String block = weathering + "cut_copper";
        return new Shaped(
                "rootboot",
                "copper/unslab/" + block,
                Category.BUILDING_BLOCKS,
                Optional.empty(),
                List.of("##"),
                Map.of('#', Ingredient.item(item(block + "_slab"))),
                new Result(item(block), 1));
    }

    /**
     * The plain copper block of a weathering state. Vanilla calls the two unweathered ones
     * {@code copper_block} and {@code waxed_copper_block} but the weathered ones {@code <state>_copper}, so
     * the id does not follow from the prefix the way every other id in the family does.
     */
    private static String weatheredBlock(String weathering) {
        boolean unweathered = weathering.isEmpty() || weathering.equals(WAXED);
        return unweathered ? weathering + "copper_block" : weathering + "copper";
    }

    private static String item(String path) {
        return "minecraft:" + path;
    }

    /** A product cut from a copper block: its registry path suffix, its yield and its book tab. */
    private record Product(String name, int count, Category category) {}
}
