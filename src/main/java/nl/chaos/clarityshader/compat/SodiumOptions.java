package nl.chaos.clarityshader.compat;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Function;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.chaos.clarityshader.ClarityShader;
import nl.chaos.clarityshader.config.Profile;
import nl.chaos.clarityshader.config.RenderScale;
import nl.chaos.clarityshader.config.ShaderConfig;

/**
 * Adds a "Clarity Shader" page to Sodium's Video Settings screen. Sodium finds this class through the
 * {@code sodium:config_api_user} mod property, so it is only ever loaded when Sodium is installed.
 */
public final class SodiumOptions implements ConfigEntryPoint {
    private static final Identifier PROFILE = ClarityShader.id("profile");
    private static final Identifier FSR_ENABLED = ClarityShader.id("fsr_enabled");

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        Function<ConfigState, Boolean> isCustom = state -> state.readEnumOption(PROFILE, Profile.class) == Profile.CUSTOM;
        Function<ConfigState, Boolean> isAuto = state -> state.readEnumOption(PROFILE, Profile.class) == Profile.AUTO;

        OptionGroupBuilder profileGroup = builder.createOptionGroup()
            .setName(Component.translatable("clarityshader.config.profile"))
            .addOption(builder.createEnumOption(PROFILE, Profile.class)
                .setName(Component.translatable("clarityshader.config.profile"))
                .setTooltip(Component.translatable("clarityshader.config.profile.tooltip"))
                .setElementNameProvider(Profile::displayName)
                .setImpact(OptionImpact.VARIES)
                .setDefaultValue(Profile.HIGH)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.PROFILE::set, ShaderConfig.PROFILE::get))
            .addOption(builder.createIntegerOption(ClarityShader.id("auto_target_fps"))
                .setName(Component.translatable("clarityshader.config.auto_target_fps"))
                .setTooltip(Component.translatable("clarityshader.config.auto_target_fps.tooltip"))
                .setRange(30, 240, 5)
                .setValueFormatter(fps -> Component.translatable("clarityshader.config.auto_target_fps.value", fps))
                .setImpact(OptionImpact.VARIES)
                .setDefaultValue(ShaderConfig.AUTO_TARGET_FPS.getDefault())
                .setEnabledProvider(isAuto, PROFILE)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.AUTO_TARGET_FPS::set, ShaderConfig.AUTO_TARGET_FPS::get))
            .addOption(builder.createBooleanOption(ClarityShader.id("ray_tracing"))
                .setName(Component.translatable("clarityshader.config.ray_tracing"))
                .setTooltip(Component.translatable("clarityshader.config.ray_tracing.tooltip"))
                .setImpact(OptionImpact.HIGH)
                .setDefaultValue(false)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.RAY_TRACING::set, ShaderConfig.RAY_TRACING::get))
            .addOption(builder.createBooleanOption(ClarityShader.id("dynamic_lights"))
                .setName(Component.translatable("clarityshader.config.dynamic_lights"))
                .setTooltip(Component.translatable("clarityshader.config.dynamic_lights.tooltip"))
                .setImpact(OptionImpact.LOW)
                .setDefaultValue(true)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.DYNAMIC_LIGHTS::set, ShaderConfig.DYNAMIC_LIGHTS::get));
        if (DistantHorizonsCompat.isLoaded()) {
            profileGroup.addOption(builder.createBooleanOption(ClarityShader.id("distant_horizons"))
                .setName(Component.translatable("clarityshader.config.distant_horizons"))
                .setTooltip(Component.translatable("clarityshader.config.distant_horizons.tooltip"))
                .setImpact(OptionImpact.HIGH)
                .setDefaultValue(true)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.DISTANT_HORIZONS::set, ShaderConfig.DISTANT_HORIZONS::get));
            profileGroup.addOption(builder.createBooleanOption(ClarityShader.id("distant_horizons_ask"))
                .setName(Component.translatable("clarityshader.config.distant_horizons_ask"))
                .setTooltip(Component.translatable("clarityshader.config.distant_horizons_ask.tooltip"))
                .setImpact(OptionImpact.LOW)
                .setDefaultValue(true)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.DISTANT_HORIZONS_ASK::set, ShaderConfig.DISTANT_HORIZONS_ASK::get));
            profileGroup.addOption(builder.createBooleanOption(ClarityShader.id("distant_horizons_shading"))
                .setName(Component.translatable("clarityshader.config.distant_horizons_shading"))
                .setTooltip(Component.translatable("clarityshader.config.distant_horizons_shading.tooltip"))
                .setImpact(OptionImpact.LOW)
                .setDefaultValue(true)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.DISTANT_HORIZONS_SHADING::set, ShaderConfig.DISTANT_HORIZONS_SHADING::get));
        }

        OptionGroupBuilder customGroup = builder.createOptionGroup()
            .setName(Component.translatable("clarityshader.config.custom"))
            .addOption(quality(builder, "ambient_occlusion", ShaderConfig.AMBIENT_OCCLUSION, OptionImpact.HIGH, isCustom))
            .addOption(quality(builder, "reflections", ShaderConfig.REFLECTIONS, OptionImpact.MEDIUM, isCustom))
            .addOption(quality(builder, "god_rays", ShaderConfig.GOD_RAYS, OptionImpact.MEDIUM, isCustom))
            .addOption(quality(builder, "bloom", ShaderConfig.BLOOM, OptionImpact.LOW, isCustom))
            .addOption(quality(builder, "shadows", ShaderConfig.SHADOWS, OptionImpact.MEDIUM, isCustom));

        OptionGroupBuilder upscalingGroup = builder.createOptionGroup()
            .setName(Component.translatable("clarityshader.config.upscaling"));
        upscalingGroup.addOption(builder.createBooleanOption(FSR_ENABLED)
            .setName(Component.translatable("clarityshader.config.fsr_enabled"))
            .setTooltip(Component.translatable("clarityshader.config.fsr_enabled.tooltip"))
            .setImpact(OptionImpact.HIGH)
            .setDefaultValue(true)
            .setStorageHandler(ShaderConfig.SPEC::save)
            .setBinding(ShaderConfig.FSR_ENABLED::set, ShaderConfig.FSR_ENABLED::get));
        Function<ConfigState, Boolean> fsrOn = state -> state.readBooleanOption(FSR_ENABLED);
        for (Profile profile : new Profile[] {Profile.LOW, Profile.MEDIUM, Profile.HIGH, Profile.MAX, Profile.CUSTOM}) {
            ModConfigSpec.EnumValue<RenderScale> value = ShaderConfig.RENDER_SCALE.get(profile);
            String key = "render_scale." + profile.name().toLowerCase(Locale.ROOT);
            upscalingGroup.addOption(builder.createEnumOption(ClarityShader.id(key), RenderScale.class)
                .setName(Component.translatable("clarityshader.config." + key))
                .setTooltip(Component.translatable("clarityshader.config.render_scale.tooltip"))
                .setElementNameProvider(RenderScale::displayName)
                .setImpact(OptionImpact.HIGH)
                .setDefaultValue(value.getDefault())
                .setEnabledProvider(fsrOn, FSR_ENABLED)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(value::set, value::get));
        }
        upscalingGroup.addOption(percent(builder, "fsr_sharpness", ShaderConfig.FSR_SHARPNESS, 0, 100).setEnabledProvider(fsrOn, FSR_ENABLED));

        OptionGroupBuilder lookGroup = builder.createOptionGroup()
            .setName(Component.translatable("clarityshader.config.look"))
            .addOption(builder.createExternalButtonOption(ClarityShader.id("reset_look"))
                .setName(Component.translatable("clarityshader.config.reset_look"))
                .setTooltip(Component.translatable("clarityshader.config.reset_look.tooltip"))
                .setScreenConsumer(SodiumOptions::confirmResetLook))
            .addOption(percent(builder, "exposure", ShaderConfig.EXPOSURE, 50, 150))
            .addOption(percent(builder, "saturation", ShaderConfig.SATURATION, 50, 150))
            .addOption(percent(builder, "contrast", ShaderConfig.CONTRAST, 50, 150))
            .addOption(percent(builder, "warmth", ShaderConfig.WARMTH, 0, 200))
            .addOption(percent(builder, "atmosphere", ShaderConfig.ATMOSPHERE, 0, 200))
            .addOption(percent(builder, "water_reflectivity", ShaderConfig.WATER_REFLECTIVITY, 0, 150))
            .addOption(percent(builder, "vignette", ShaderConfig.VIGNETTE, 0, 100))
            .addOption(builder.createBooleanOption(ClarityShader.id("weather_integration"))
                .setName(Component.translatable("clarityshader.config.weather_integration"))
                .setTooltip(Component.translatable("clarityshader.config.weather_integration.tooltip"))
                .setImpact(OptionImpact.LOW)
                .setDefaultValue(true)
                .setStorageHandler(ShaderConfig.SPEC::save)
                .setBinding(ShaderConfig.WEATHER_INTEGRATION::set, ShaderConfig.WEATHER_INTEGRATION::get));

        builder.registerOwnModOptions()
            .setName("Clarity Shader")
            // Sodium shows this next to the name in its Video Settings sidebar; non-tinted keeps the icon's own colours.
            .setNonTintedIcon(ClarityShader.id("textures/gui/icon.png"))
            .addPage(builder.createOptionPage()
                .setName(Component.translatable("clarityshader.config.page.quality"))
                .addOptionGroup(profileGroup)
                .addOptionGroup(customGroup)
                .addOptionGroup(upscalingGroup))
            .addPage(builder.createOptionPage()
                .setName(Component.translatable("clarityshader.config.look"))
                .addOptionGroup(lookGroup));
    }

    private static void confirmResetLook(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(
            reset -> {
                if (reset) {
                    ShaderConfig.resetLook();
                }
                // Reopen Sodium's settings so the sliders show the new values.
                minecraft.setScreen(reset ? net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen.createScreen(screenBefore(parent)) : parent);
            },
            Component.translatable("clarityshader.config.reset_look"),
            Component.translatable("clarityshader.config.reset_look.confirm")
        ));
    }

    // The screen Sodium's settings were opened from, so "Done" after the reopen still leads back there.
    private static Screen screenBefore(Screen sodiumScreen) {
        try {
            Field field = sodiumScreen.getClass().getDeclaredField("prevScreen");
            field.setAccessible(true);
            Screen previous = (Screen) field.get(sodiumScreen);
            return previous != null ? previous : sodiumScreen;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return sodiumScreen;
        }
    }

    private static IntegerOptionBuilder quality(
        ConfigBuilder builder, String key, ModConfigSpec.IntValue value, OptionImpact impact, Function<ConfigState, Boolean> isCustom
    ) {
        return builder.createIntegerOption(ClarityShader.id(key))
            .setName(Component.translatable("clarityshader.config." + key))
            .setTooltip(Component.translatable("clarityshader.config." + key + ".tooltip"))
            .setRange(0, 4, 1)
            .setValueFormatter(level -> Component.translatable("clarityshader.quality." + level))
            .setImpact(impact)
            .setDefaultValue(value.getDefault())
            .setEnabledProvider(isCustom, PROFILE)
            .setStorageHandler(ShaderConfig.SPEC::save)
            .setBinding(value::set, value::get);
    }

    private static IntegerOptionBuilder percent(ConfigBuilder builder, String key, ModConfigSpec.IntValue value, int min, int max) {
        return builder.createIntegerOption(ClarityShader.id(key))
            .setName(Component.translatable("clarityshader.config." + key))
            .setTooltip(Component.translatable("clarityshader.config." + key + ".tooltip"))
            .setRange(min, max, 5)
            .setValueFormatter(v -> Component.literal(v + "%"))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(value.getDefault())
            .setStorageHandler(ShaderConfig.SPEC::save)
            .setBinding(value::set, value::get);
    }
}
