package corgitaco.riderdenier.mixin;

import corgitaco.riderdenier.ConfigSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public World level;

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private void denyRiding(Entity entityBeingMounted, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation dimensionLocation = this.level.dimension().location();
        if (ConfigSerializer.BLACKLISTED_ENTITIES.containsKey(dimensionLocation)) {
            if (ConfigSerializer.BLACKLISTED_ENTITIES.get(dimensionLocation).contains(entityBeingMounted.getType())) {
                if (((Entity) (Object) this) instanceof PlayerEntity) {
                    ((PlayerEntity) (Object) this).displayClientMessage(new TranslationTextComponent("mount.failed"), true);
                }
                cir.setReturnValue(false);
            }
        }
    }
}
