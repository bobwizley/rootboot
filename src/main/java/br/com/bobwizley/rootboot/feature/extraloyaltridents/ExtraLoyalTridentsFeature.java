package br.com.bobwizley.rootboot.feature.extraloyaltridents;

import br.com.bobwizley.rootboot.feature.Feature;

public final class ExtraLoyalTridentsFeature implements Feature {

    public static final String ID = "extra_loyal_tridents";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        ExtraLoyalTridents.enable();
    }
}
