package br.com.bobwizley.rootboot;

import br.com.bobwizley.rootboot.config.RootBootConfig;
import br.com.bobwizley.rootboot.enchantment.RootBootEnchantmentEffects;
import br.com.bobwizley.rootboot.feature.FeatureRegistry;
import br.com.bobwizley.rootboot.feature.biomediscovery.BiomeDiscoveryFeature;
import br.com.bobwizley.rootboot.feature.cropsexperience.CropsExperienceFeature;
import br.com.bobwizley.rootboot.feature.dayannouncement.DayAnnouncementFeature;
import br.com.bobwizley.rootboot.feature.deathitemprotection.DeathItemProtectionFeature;
import br.com.bobwizley.rootboot.feature.halfhealthbabies.HalfHealthBabies;
import br.com.bobwizley.rootboot.feature.halfhealthbabies.HalfHealthBabiesFeature;
import br.com.bobwizley.rootboot.feature.homingexperienceorb.HomingExperienceOrbFeature;
import br.com.bobwizley.rootboot.feature.levelmilestone.LevelMilestoneFeature;
import br.com.bobwizley.rootboot.feature.majoreventdiscovery.MajorEventDiscoveryFeature;
import br.com.bobwizley.rootboot.feature.timeoffset.TimeOffsetFeature;
import br.com.bobwizley.rootboot.feature.timeoffset.TimeOffsetWorldInitializer;
import br.com.bobwizley.rootboot.feature.heavyfoot.HeavyfootFeature;
import br.com.bobwizley.rootboot.feature.lightfoot.LightfootFeature;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RootBoot implements ModInitializer {

    public static final String MOD_ID = "rootboot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RootBootConfig config;

    public static RootBootConfig config() {
        if (config == null) {
            config = RootBootConfig.load(configPath());
        }
        return config;
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
    }

    @Override
    public void onInitialize() {
        RootBootConfig cfg = config();

        RootBootEnchantmentEffects.register();

        FeatureRegistry registry = new FeatureRegistry(List.of(
                new TimeOffsetFeature(),
                new DayAnnouncementFeature(),
                new LevelMilestoneFeature(),
                new HomingExperienceOrbFeature(),
                new CropsExperienceFeature(),
                new DeathItemProtectionFeature(),
                new BiomeDiscoveryFeature(),
                new MajorEventDiscoveryFeature(),
                new HalfHealthBabiesFeature(),
                new HeavyfootFeature(cfg.heavyfootRadius()),
                new LightfootFeature()));
        if (cfg.ensureKeys(registry.featureIds())) {
            cfg.save(configPath());
        }

        List<String> registered = registry.registerEnabled(cfg);
        LOGGER.info("RootBoot registered {} behavior feature(s): {}", registered.size(), registered);

        // Persistence exception: the baby reduction is saved inside the entity, so the load path
        // must reconcile it even when Half-health Babies is disabled. Leaving that to the entity's
        // first tick would keep a baby reduced — and let it be saved reduced again — for as long as
        // its chunk stays loaded outside the simulation distance.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity livingEntity) {
                HalfHealthBabies.applyCurrentPolicy(livingEntity);
            }
        });

        // Persistence exception: even when Time Offset is disabled, a world's first
        // initialization must be recorded as evaluated so that enabling the feature later
        // never retroactively shifts an already-created world.
        if (!cfg.isEnabled(TimeOffsetFeature.ID)) {
            ServerLifecycleEvents.SERVER_STARTED.register(server ->
                    TimeOffsetWorldInitializer.firstInit(server, false));
        }
    }
}
