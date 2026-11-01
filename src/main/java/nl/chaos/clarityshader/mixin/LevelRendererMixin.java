package nl.chaos.clarityshader.mixin;

//? if >=26.3 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.LevelRenderer;
import nl.chaos.clarityshader.client.ShaderRenderer;
import nl.chaos.clarityshader.compat.McCompat;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// From 26.3 the level's opaque and translucent geometry share one render pass, and nothing can be copied while a
// pass is open, so the depth snapshots the effects need are out of reach. These hooks take them between passes: by
// splitting the pass before translucent geometry and again right after translucent terrain (before particles and
// clouds), or, with improved transparency (OIT), around the OIT stage, which leaves the depth buffer alone anyway.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique
    private static @Nullable RenderPass clarityshader$ownPass;

    @ModifyVariable(method = "executeClassicTransparency", at = @At("HEAD"), argsOnly = true)
    private RenderPass clarityshader$splitBeforeTranslucent(RenderPass pass) {
        if (!ShaderRenderer.wantsOpaqueSnapshot()) {
            return pass;
        }
        // Closing twice is harmless, so the caller's try-with-resources closing it again does nothing.
        pass.close();
        ShaderRenderer.captureOpaque();
        return clarityshader$reopen();
    }

    // Where 26.2 fired AfterTranslucentBlocks: translucent terrain is in, the clouds (which also write depth but should
    // stay sky to the effects) are not yet.
    @ModifyVariable(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 1, shift = At.Shift.AFTER),
        argsOnly = true
    )
    private RenderPass clarityshader$splitAfterTranslucentTerrain(RenderPass pass) {
        if (pass != clarityshader$ownPass) {
            return pass;
        }
        pass.close();
        ShaderRenderer.captureTranslucent();
        return clarityshader$reopen();
    }

    @Inject(method = "executeClassicTransparency", at = @At("TAIL"))
    private void clarityshader$closeOwnPass(CallbackInfo ci) {
        if (clarityshader$ownPass != null) {
            clarityshader$ownPass.close();
            clarityshader$ownPass = null;
        }
    }

    @Inject(method = "executeOit", at = @At("HEAD"))
    private void clarityshader$beforeOit(CallbackInfo ci) {
        ShaderRenderer.captureOpaque();
    }

    @Inject(method = "executeOit", at = @At("TAIL"))
    private void clarityshader$afterOit(CallbackInfo ci) {
        ShaderRenderer.captureOit();
    }

    @Unique
    private static RenderPass clarityshader$reopen() {
        RenderTarget main = McCompat.mainTarget();
        RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Main (continued after a Clarity Shader snapshot)", main.getColorTextureView(), Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty()
        );
        RenderSystem.bindDefaultUniforms(pass);
        clarityshader$ownPass = pass;
        return pass;
    }
}
*///?}
