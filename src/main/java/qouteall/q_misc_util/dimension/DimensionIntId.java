package qouteall.q_misc_util.dimension;

import com.mojang.logging.LogUtils;
import de.nick1st.imm_ptl.events.ClientExitEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.MiscNetworking;

import java.util.HashSet;

public class DimensionIntId {
    private static final Logger LOGGER = LogUtils.getLogger();
//    public static final ResourceLocation DYNAMIC_UPDATE_EVENT_EARLY_PHASE =
//        McHelper.newResourceLocation("iportal:early_phase");
    
    public static DimIntIdMap clientRecord;
    
    public static void init() {
        // make sure that dimension int id updates before global portal storage update
 //       DimensionAPI.SERVER_DIMENSION_DYNAMIC_UPDATE_EVENT.addPhaseOrdering(
 //           DYNAMIC_UPDATE_EVENT_EARLY_PHASE,
 //           Event.DEFAULT_PHASE
 //       );
 //
 //       DimensionAPI.SERVER_DIMENSION_DYNAMIC_UPDATE_EVENT.register(
 //           DYNAMIC_UPDATE_EVENT_EARLY_PHASE,
 //           (server, dimensions) -> {
 //               onServerDimensionChanged(server);
 //           }
 //       );
    }
    
    //@Environment(EnvType.CLIENT)
    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(ClientExitEvent.class, (e) -> DimensionIntId.onClientExit());
    }
    
    //@Environment(EnvType.CLIENT)
    private static void onClientExit() {
        clientRecord = null;
    }

    /**
     * Note this should not be used in networking thread.
     */
    //@Environment(EnvType.CLIENT)
    public static @NotNull DimIntIdMap getClientMap() {
        Validate.notNull(clientRecord,
            "Client dim id record is not yet synced. This should not be used in networking thread."
        );
        return clientRecord;
    }
    
    public static @NotNull DimIntIdMap getServerMap(MinecraftServer server) {
        IPPerServerInfo perServerInfo = IPPerServerInfo.of(server);
        DimIntIdMap rec = perServerInfo.dimIntIdMap;
        Validate.notNull(rec, "Server dim id record is not yet initialized");
        return rec;
    }
    
    public static void onServerStarted(MinecraftServer server) {
        DimIntIdMap rec = new DimIntIdMap();
        
        fillInVanillaDimIds(rec);
        
        for (ServerLevel world : server.getAllLevels()) {
            ResourceKey<Level> dimId = world.dimension();
            if (!rec.containsDimId(dimId)) {
                rec.add(dimId, rec.getNextIntegerId());
            }
        }
        
        IPPerServerInfo perServerInfo = IPPerServerInfo.of(server);
        perServerInfo.dimIntIdMap = rec;
        LOGGER.info("Server dimension integer id mapping:\n{}", rec);
    }
    
    private static void fillInVanillaDimIds(DimIntIdMap rec) {
        if (!rec.containsDimId(Level.OVERWORLD)) {
            rec.add(Level.OVERWORLD, 0);
        }
        if (!rec.containsDimId(Level.NETHER)) {
            rec.add(Level.NETHER, -1);
        }
        if (!rec.containsDimId(Level.END)) {
            rec.add(Level.END, 1);
        }
    }
    
    public static void onServerDimensionChanged(MinecraftServer server) {
        DimIntIdMap map = getServerMap(server);
        
        for (ResourceKey<Level> levelKey : server.levelKeys()) {
            if (!map.containsDimId(levelKey)) {
                map.add(levelKey, map.getNextIntegerId());
            }
        }
        
        HashSet<ResourceKey<Level>> usedDimKeys = new HashSet<>(server.levelKeys());
        
        // avoid vanilla dimensions from being removed from mapping
        usedDimKeys.add(Level.OVERWORLD);
        usedDimKeys.add(Level.NETHER);
        usedDimKeys.add(Level.END);
        
        map.removeUnused(usedDimKeys);
        
        LOGGER.info("Current dimension integer id mapping:\n{}", map);
        
        var packet = MiscNetworking.DimIdSyncPacket.createPacket(server);
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }
}
