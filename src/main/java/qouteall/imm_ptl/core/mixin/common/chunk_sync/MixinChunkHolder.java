package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.ducks.IEChunkHolder;
import qouteall.imm_ptl.core.network.PacketRedirection;

import java.util.List;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder implements IEChunkHolder {
    
    @Shadow
    @Final
    private LevelHeightAccessor levelHeightAccessor;
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyVariable(
        method = "broadcast",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Packet<?> modifyPacket(Packet<?> packet) {
        ServerLevel serverWorld = (ServerLevel) levelHeightAccessor;
        return PacketRedirection.createRedirectedMessage(
            serverWorld.getServer(),
            serverWorld.dimension(),
            ((Packet) packet)
        );
    }
    
    /**
     * Does not mixin {@link net.minecraft.server.level.ChunkMap#getPlayers(ChunkPos, boolean)}
     * because the current chunk map tracking implementation should coexist with vanilla tracking
     * and avoid deeply interfering with vanilla chunk tracking.
     */
    @Redirect(
        method = "broadcastChanges",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder$PlayerProvider;getPlayers(Lnet/minecraft/world/level/ChunkPos;Z)Ljava/util/List;"
        )
    )
    private List<ServerPlayer> redirectGetPlayers(ChunkHolder.PlayerProvider playerProvider, ChunkPos chunkPos, boolean boundaryOnly) {
        return ImmPtlChunkTracking.getPlayersViewingChunk(
            ((Level) levelHeightAccessor).dimension(),
            chunkPos.x, chunkPos.z,
            boundaryOnly
        );
    }
}
