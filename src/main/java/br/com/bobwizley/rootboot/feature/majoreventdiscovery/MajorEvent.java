package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import com.mojang.serialization.Codec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;

/**
 * The two milestones a player discovers at most once per world. The serialized name is the
 * persisted identity and also roots the translation keys, so renaming one invalidates existing
 * discoveries.
 */
public enum MajorEvent implements StringRepresentable {

    END("end", 0xAA00AA),
    WITHER("wither", 0xAA0000);

    public static final Codec<MajorEvent> CODEC = StringRepresentable.fromEnum(MajorEvent::values);

    private final String serializedName;
    private final int color;

    MajorEvent(String serializedName, int color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public int color() {
        return color;
    }

    public String titleKey() {
        return "message.rootboot.major_event." + serializedName + ".title";
    }

    public String subtitleKey() {
        return "message.rootboot.major_event." + serializedName + ".subtitle";
    }

    public SoundEvent sound() {
        return switch (this) {
            case END -> SoundEvents.END_PORTAL_SPAWN;
            case WITHER -> SoundEvents.WITHER_SPAWN;
        };
    }
}
