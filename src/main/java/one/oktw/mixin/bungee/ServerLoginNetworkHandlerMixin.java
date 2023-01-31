package one.oktw.mixin.bungee;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import one.oktw.interfaces.BungeeClientConnection;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    @Final
    public ClientConnection connection;

    @Shadow
    private GameProfile profile;


    @Inject(method = "onHello", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;profile:Lcom/mojang/authlib/GameProfile;",
            shift = At.Shift.AFTER))
    private void initUuid(CallbackInfo ci) {
        // override game profile with saved information:
        this.profile = new GameProfile(((BungeeClientConnection) connection).getSpoofedUUID(), this.profile.getName());

        if (((BungeeClientConnection) connection).getSpoofedProfile() != null) {
            for (Property property : ((BungeeClientConnection) connection).getSpoofedProfile()) {
                this.profile.getProperties().put(property.getName(), property);
            }
        }
    }

    @Redirect(method = "onHello", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z"))
    private boolean skipKeyPacket(MinecraftServer minecraftServer) {
        return false;
    }

    @Redirect(method = "acceptPlayer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;getVerifiedPublicKey(Lnet/minecraft/network/encryption/PlayerPublicKey$PublicKeyData;Ljava/util/UUID;Lnet/minecraft/network/encryption/SignatureVerifier;Z)Lnet/minecraft/network/encryption/PlayerPublicKey;")
    )
    private PlayerPublicKey skipChatKey(PlayerPublicKey.PublicKeyData publicKeyData, UUID playerUuid, SignatureVerifier servicesSignatureVerifier, boolean shouldThrowOnMissingKey) {
        return null;
    }
}
