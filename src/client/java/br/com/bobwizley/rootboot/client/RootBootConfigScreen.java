package br.com.bobwizley.rootboot.client;

import br.com.bobwizley.rootboot.RootBoot;
import br.com.bobwizley.rootboot.client.feature.jukeboxmusicoverride.JukeboxMusicOverrideFeature;
import br.com.bobwizley.rootboot.client.feature.localdeathsound.LocalDeathSoundFeature;
import br.com.bobwizley.rootboot.client.feature.lowhealthsound.LowHealthSoundFeature;
import br.com.bobwizley.rootboot.client.feature.stopmusicondeath.StopMusicOnDeathFeature;
import br.com.bobwizley.rootboot.config.RootBootConfig;
import br.com.bobwizley.rootboot.feature.biomediscovery.BiomeDiscoveryFeature;
import br.com.bobwizley.rootboot.feature.cropsexperience.CropsExperienceFeature;
import br.com.bobwizley.rootboot.feature.dayannouncement.DayAnnouncementFeature;
import br.com.bobwizley.rootboot.feature.deathitemprotection.DeathItemProtectionFeature;
import br.com.bobwizley.rootboot.feature.halfhealthbabies.HalfHealthBabiesFeature;
import br.com.bobwizley.rootboot.feature.heavyfoot.HeavyfootFeature;
import br.com.bobwizley.rootboot.feature.homingexperienceorb.HomingExperienceOrbFeature;
import br.com.bobwizley.rootboot.feature.levelmilestone.LevelMilestoneFeature;
import br.com.bobwizley.rootboot.feature.lightfoot.LightfootFeature;
import br.com.bobwizley.rootboot.feature.majoreventdiscovery.MajorEventDiscoveryFeature;
import br.com.bobwizley.rootboot.feature.timeoffset.TimeOffsetFeature;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RootBootConfigScreen {

    private RootBootConfigScreen() {
    }

    public static Screen create(Screen parent) {
        RootBootConfig config = RootBoot.config();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.rootboot.config"))
                .setSavingRunnable(() -> config.save(RootBoot.configPath()));

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("category.rootboot.general"));

        general.addEntry(entries.startTextDescription(
                Component.translatable("text.rootboot.restart_required")).build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.time_offset"),
                        config.isEnabled(TimeOffsetFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.time_offset.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(TimeOffsetFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.day_announcement"),
                        config.isEnabled(DayAnnouncementFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.day_announcement.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(DayAnnouncementFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.level_milestone"),
                        config.isEnabled(LevelMilestoneFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.level_milestone.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(LevelMilestoneFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.homing_experience_orb"),
                        config.isEnabled(HomingExperienceOrbFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable(
                        "option.rootboot.homing_experience_orb.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(HomingExperienceOrbFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.crops_experience"),
                        config.isEnabled(CropsExperienceFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.crops_experience.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(CropsExperienceFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.death_item_protection"),
                        config.isEnabled(DeathItemProtectionFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable(
                        "option.rootboot.death_item_protection.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(DeathItemProtectionFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.biome_discovery"),
                        config.isEnabled(BiomeDiscoveryFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.biome_discovery.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(BiomeDiscoveryFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.major_event_discovery"),
                        config.isEnabled(MajorEventDiscoveryFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.major_event_discovery.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(MajorEventDiscoveryFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.half_health_babies"),
                        config.isEnabled(HalfHealthBabiesFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.half_health_babies.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(HalfHealthBabiesFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.heavyfoot"),
                        config.isEnabled(HeavyfootFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.heavyfoot.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(HeavyfootFeature.ID, value))
                .build());

        general.addEntry(entries.startIntSlider(
                        Component.translatable("option.rootboot.heavyfoot_radius"),
                        config.heavyfootRadius(),
                        RootBootConfig.HEAVYFOOT_RADIUS_MIN,
                        RootBootConfig.HEAVYFOOT_RADIUS_MAX)
                .setDefaultValue(RootBootConfig.HEAVYFOOT_RADIUS_DEFAULT)
                .setTooltip(Component.translatable("option.rootboot.heavyfoot_radius.tooltip"))
                .setSaveConsumer(config::setHeavyfootRadius)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.lightfoot"),
                        config.isEnabled(LightfootFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.lightfoot.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(LightfootFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.jukebox_music_override"),
                        config.isEnabled(JukeboxMusicOverrideFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.jukebox_music_override.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(JukeboxMusicOverrideFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.local_death_sound"),
                        config.isEnabled(LocalDeathSoundFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.local_death_sound.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(LocalDeathSoundFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.stop_music_on_death"),
                        config.isEnabled(StopMusicOnDeathFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.stop_music_on_death.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(StopMusicOnDeathFeature.ID, value))
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.translatable("option.rootboot.low_health_sound"),
                        config.isEnabled(LowHealthSoundFeature.ID))
                .setDefaultValue(true)
                .setTooltip(Component.translatable("option.rootboot.low_health_sound.tooltip"))
                .setSaveConsumer(value -> config.setEnabled(LowHealthSoundFeature.ID, value))
                .build());

        return builder.build();
    }
}
