package br.com.bobwizley.rootboot.feature.trimmedarmoredpiglins;

import br.com.bobwizley.rootboot.feature.Feature;

/**
 * Trimmed Armored Piglins: armor a piglin is born wearing may come out trimmed.
 */
public final class TrimmedArmoredPiglinsFeature implements Feature {

    public static final String ID = "trimmed_armored_piglins";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        TrimmedArmoredPiglins.enable();
    }
}
