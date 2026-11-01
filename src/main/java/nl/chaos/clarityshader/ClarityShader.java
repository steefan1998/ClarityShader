package nl.chaos.clarityshader;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import nl.chaos.clarityshader.client.DevCapture;
import nl.chaos.clarityshader.client.DistantHorizonsPrompt;
import nl.chaos.clarityshader.client.ShaderRenderer;
import nl.chaos.clarityshader.client.VideoSettingsButton;
import nl.chaos.clarityshader.compat.DistantHorizonsCompat;
import nl.chaos.clarityshader.config.Profile;
import nl.chaos.clarityshader.config.ShaderConfig;
import org.slf4j.Logger;

@Mod(value = ClarityShader.MOD_ID, dist = Dist.CLIENT)
public final class ClarityShader {
    public static final String MOD_ID = "clarityshader";
    private static final Logger LOGGER = LogUtils.getLogger();

    //? if >=26.1 {
    private static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(id("keys"));
    //?} else
    /*private static final String KEY_CATEGORY = "key.category.clarityshader.keys";*/
    private static final KeyMapping CYCLE_PROFILE = new KeyMapping(
        "key.clarityshader.cycle_profile", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, KEY_CATEGORY
    );

    public ClarityShader(IEventBus modBus, ModContainer container) {
        migrateOldConfig();
        container.registerConfig(ModConfig.Type.CLIENT, ShaderConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(ClarityShader::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClarityShader::clientTick);
        ShaderRenderer.init();
        DistantHorizonsCompat.init();
        VideoSettingsButton.init();
        DevCapture.init();
    }

    // The mod was called Vanilla Shader before; carry its settings over the first time Clarity Shader starts.
    private static void migrateOldConfig() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path oldConfig = configDir.resolve("vanillashader-client.toml");
        Path newConfig = configDir.resolve(MOD_ID + "-client.toml");
        if (Files.exists(oldConfig) && !Files.exists(newConfig)) {
            try {
                Files.copy(oldConfig, newConfig);
            } catch (IOException e) {
                LOGGER.warn("Could not copy the old Vanilla Shader settings", e);
            }
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        //? if >=26.1
        event.registerCategory(KEY_CATEGORY);
        event.register(CYCLE_PROFILE);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        DistantHorizonsCompat.tick();
        DistantHorizonsPrompt.tick(minecraft);
        while (CYCLE_PROFILE.consumeClick()) {
            Profile next = ShaderConfig.PROFILE.get().next();
            ShaderConfig.setProfile(next);
            if (minecraft.player != null) {
                //? if >=26.1 {
                minecraft.player.sendOverlayMessage(Component.translatable("clarityshader.message.profile", next.displayName()));
                //?} else
                /*minecraft.player.displayClientMessage(Component.translatable("clarityshader.message.profile", next.displayName()), true);*/
            }
        }
    }
}
