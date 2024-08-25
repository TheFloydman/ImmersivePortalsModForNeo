package qouteall.imm_ptl.core.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("resource")
public class IPModInfoChecking {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final boolean IS_TEST = false;
    
    public static record ModIncompatInfo(
        String modId,
        String modName,
        @Nullable String startVersion,
        @Nullable String endVersion,
        @Nullable String desc,
        @Nullable String link
    ) {
        boolean isModLoadedWithinVersion() {
            return O_O.isModLoadedWithinVersion(modId, startVersion, endVersion);
        }
        
        String getVersionRangeStr() {
            if (startVersion != null) {
                if (endVersion != null) {
                    return startVersion + "-" + endVersion;
                }
                else {
                    return startVersion + "+";
                }
            }
            else {
                Validate.notNull(endVersion, "invalid version range");
                return "-" + endVersion;
            }
        }
        
    }
    
    public static final class ImmPtlInfo {
        public String latestReleaseVersion;
        public List<String> previewVersions;
        public List<ModIncompatInfo> severelyIncompatible;
        public List<ModIncompatInfo> incompatible;
        public List<String> incompatibleShaderpacks;
        
        public ImmPtlInfo(String latestReleaseVersion, List<ModIncompatInfo> severelyIncompatible, List<ModIncompatInfo> incompatible) {
            this.latestReleaseVersion = latestReleaseVersion;
            this.severelyIncompatible = severelyIncompatible;
            this.incompatible = incompatible;
        }
        
        @Override
        public String toString() {
            return "ImmPtlInfo{" +
                "latestReleaseVersion='" + latestReleaseVersion + '\'' +
                ", severelyIncompatible=" + severelyIncompatible +
                ", incompatible=" + incompatible +
                '}';
        }
    }
    
    // NOTE do not run it on render thread
    @Nullable
    public static ImmPtlInfo fetchImmPtlInfoFromInternet() {
        if (IS_TEST) {
            return new ImmPtlInfo(
                "1.0.0",
                List.of(
                    new ModIncompatInfo(
                        "fabric-api", "modname",
                        "1.0.0", "1.1.0", "desc",
                        "https://github.com/iPortalTeam/ImmersivePortalsMod/discussions"
                    )
                ),
                List.of()
            );
        }
        
        String url = O_O.getImmPtlModInfoUrl();
        
        if (url == null) {
            LOGGER.info("Not fetching iPortal mod info");
            return null;
        }
        
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Immersive Portals mod")
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() != 200) {
                LOGGER.error("Failed to fetch iPortal mod info {}", response.statusCode());
                return null;
            }
            
            String jsonStr = response.body();
            ImmPtlInfo immPtlInfo = Helper.gson.fromJson(jsonStr, ImmPtlInfo.class);
            LOGGER.info("iPortal mod info fetched");
            return immPtlInfo;
        }
        catch (Throwable e) {
            LOGGER.error("Failed to fetch iPortal mod info", e);
            return null;
        }
    }
    
    //@OnlyIn(Dist.CLIENT)
    public static void initClient() {
        Util.backgroundExecutor().execute(() -> {
            if (!IPGlobal.checkModInfoFromInternet) {
                return;
            }
            
            ImmPtlInfo immPtlInfo = fetchImmPtlInfoFromInternet();
            
            if (immPtlInfo == null) {
                return;
            }
            
            incompatibleShaderpacks = immPtlInfo.incompatibleShaderpacks;
            
            List<MutableComponent> severeIncompatWarningText =
                getSevereIncompatWarningText(immPtlInfo);
            
            if (!severeIncompatWarningText.isEmpty()) {
                MutableComponent toDirectlyDisplay = severeIncompatWarningText.stream()
                    .reduce(Component.empty(), (a, b)->a.append("\n").append(b));
                
                // TODO display before entering world
            }
            
            IPGlobal.CLIENT_TASK_LIST.addTask(MyTaskList.withDelayCondition(
                () -> Minecraft.getInstance().level == null,
                MyTaskList.oneShotTask(() -> {
                    List<MutableComponent> texts = new ArrayList<>();
                    
                    if (IPGlobal.enableUpdateNotification) {
                        if (O_O.shouldUpdateImmPtl(immPtlInfo.latestReleaseVersion)) {
                            MutableComponent text1 = Component.translatable(
                                "imm_ptl.new_version_available",
                                immPtlInfo.latestReleaseVersion
                            );
                            text1.append(McHelper.getLinkText(O_O.getModDownloadLink()));
                            
                            text1.append(Component.literal("  "));
                            text1.append(IPMcHelper.getDisableUpdateCheckText());
                            
                            texts.add(text1);
                        }
                    }
                    
                    if (IPConfig.getConfig().shouldDisplayWarning("preview")) {
                        if (immPtlInfo.previewVersions.contains(O_O.getImmPtlVersionStr())) {
                            MutableComponent text1 = Component.translatable("imm_ptl.preview_warning")
                                .append(McHelper.getLinkText(O_O.getIssueLink()))
                                .append(" ")
                                .append(IPMcHelper.getDisableWarningText("preview"));
                            
                            texts.add(text1);
                        }
                    }
                    
                    texts.addAll(severeIncompatWarningText);
                    
                    for (ModIncompatInfo info : immPtlInfo.incompatible) {
                        if (info != null && info.isModLoadedWithinVersion()) {
                            if (IPConfig.getConfig().shouldDisplayWarning(info.modId)) {
                                MutableComponent text1 = Component.translatable("imm_ptl.incompatible", info.modName, info.modId)
                                    .withStyle(ChatFormatting.RED)
                                    .append(IPMcHelper.getDisableWarningText(info.modId));
                                
                                if (info.desc != null) {
                                    text1.append(Component.literal(" " + info.desc + " "));
                                }
                                
                                if (info.link != null) {
                                    text1.append(McHelper.getLinkText(" " + info.link));
                                }
                                
                                texts.add(text1);
                            }
                        }
                    }
                    
                    for (Component text : texts) {
                        CHelper.printChat(text);
                    }
                })
            ));
        });
        
        IPGlobal.CLIENT_TASK_LIST.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPConfig.getConfig().shouldDisplayWarning("many_mods") && FMLEnvironment.production) {
                    List<ModContainer> topLevelMods = ModList.get().getSortedMods(); // TODO @Nick1st Fix this up
//                    FabricLoader.getInstance().getAllMods().stream()
//                        .filter(modContainer -> modContainer.getContainingMod().isEmpty()).toList();
                    
                    if (topLevelMods.size() > 20) {
                        CHelper.printChat(Component.literal(
                            "[Immersive Portals] WARNING: You are using many mods. It's likely that one of them has compatibility issues with Immersive Portals. " +
                                "If you are sure that there is no compatibility issue, disable this warning."
                        ).withStyle(ChatFormatting.RED).append(IPMcHelper.getDisableWarningText("many_mods")));
                    }
                }
            })
        ));
        
    }
    
    private static List<MutableComponent> getSevereIncompatWarningText(
        ImmPtlInfo immPtlInfo
    ) {
        List<MutableComponent> texts = new ArrayList<>();
        for (ModIncompatInfo info : immPtlInfo.severelyIncompatible) {
            if (info != null && info.isModLoadedWithinVersion()) {
                MutableComponent text1;
                if (info.startVersion != null || info.endVersion != null) {
                    text1 = Component.translatable(
                        "imm_ptl.severely_incompatible_within_version",
                        info.modName, info.modId,
                        info.getVersionRangeStr()
                    ).withStyle(ChatFormatting.RED);
                }
                else {
                    text1 = Component.translatable("imm_ptl.severely_incompatible", info.modName, info.modId)
                        .withStyle(ChatFormatting.RED);
                }
                
                if (info.desc != null) {
                    text1.append(Component.literal(" " + info.desc + " "));
                }
                
                if (info.link != null) {
                    text1.append(Component.literal(" "));
                    text1.append(McHelper.getLinkText(info.link));
                }
                
                texts.add(text1);
            }
        }
        
        return texts;
    }
    
    public static void initDedicatedServer() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            MinecraftServer server = event.getServer();
            if (!IPGlobal.checkModInfoFromInternet) {
                return;
            }
            
            ImmPtlInfo immPtlInfo = fetchImmPtlInfoFromInternet();
            
            if (immPtlInfo == null) {
                return;
            }
            
            ServerTaskList.of(server).addOneShotTask(() -> {
                if (IPGlobal.enableUpdateNotification) {
                    if (O_O.shouldUpdateImmPtl(immPtlInfo.latestReleaseVersion)) {
                        LOGGER.info("[Immersive Portals] A new version is available. It is recommended to update to " + immPtlInfo.latestReleaseVersion);
                    }
                }
                
                for (ModIncompatInfo mod : immPtlInfo.severelyIncompatible) {
                    if (mod != null && mod.isModLoadedWithinVersion()) {
                        MutableComponent text1;
                        if (mod.startVersion != null || mod.endVersion != null) {
                            text1 = Component.translatable(
                                "imm_ptl.severely_incompatible_within_version",
                                mod.modName, mod.modId,
                                mod.getVersionRangeStr()
                            ).withStyle(ChatFormatting.RED);
                            LOGGER.error(
                                "[Immersive Portals] Detected an incompatible mod: {} {} version range: {} description: {} link: {}",
                                mod.modName, mod.modId, mod.getVersionRangeStr(), mod.desc, mod.link
                            );
                        }
                        else {
                            text1 = Component.translatable("imm_ptl.severely_incompatible", mod.modName, mod.modId)
                                .withStyle(ChatFormatting.RED);
                            LOGGER.error(
                                "[Immersive Portals] Detected an incompatible mod: {} {} description: {} link: {}",
                                mod.modName, mod.modId, mod.desc, mod.link
                            );
                        }
                        
                        if (mod.desc != null) {
                            text1.append(Component.literal(" " + mod.desc + " "));
                        }
                        
                        if (mod.link != null) {
                            text1.append(Component.literal(" "));
                            text1.append(McHelper.getLinkText(mod.link));
                        }
                        
                        McHelper.sendMessageToFirstLoggedPlayer(
                            server,
                            Component.translatable("imm_ptl.message_from_server")
                                .append(text1)
                        );
                    }
                }
                
                for (ModIncompatInfo mod : immPtlInfo.incompatible) {
                    if (mod != null && mod.isModLoadedWithinVersion()) {
                        if (IPConfig.getConfig().shouldDisplayWarning(mod.modId)) {
                            MutableComponent text1 = Component.translatable("imm_ptl.incompatible", mod.modName, mod.modId)
                                .withStyle(ChatFormatting.RED);
                            
                            if (mod.desc != null) {
                                text1.append(Component.literal(" " + mod.desc + " "));
                            }
                            
                            if (mod.link != null) {
                                text1.append(McHelper.getLinkText(" " + mod.link));
                            }
                            
                            LOGGER.error("[Immersive Portals] Detected a mildly incompatible mod: {} {} description: {} link: {}",
                                mod.modName, mod.modId, mod.desc, mod.link
                            );
                            McHelper.sendMessageToFirstLoggedPlayer(
                                server,
                                Component.translatable("imm_ptl.message_from_server")
                                    .append(text1)
                            );
                        }
                    }
                }
            });
        });
    }
    
    @Nullable
    private static List<String> incompatibleShaderpacks;
    @Nullable
    private static String lastShaderpackName;

    public static void checkShaderpack() {
        if (!IPConfig.getConfig().shaderpackWarning) {
            return;
        }

        String shaderpackName = IrisInterface.invoker.getShaderpackName();
        if (!Objects.equals(lastShaderpackName, shaderpackName)) {
            lastShaderpackName = shaderpackName;

            if (shaderpackName != null) {
                if (incompatibleShaderpacks != null) {
                    boolean incompatible = incompatibleShaderpacks.stream().anyMatch(
                        n -> shaderpackName.toLowerCase().contains(n.toLowerCase())
                    );
                    if (incompatible) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.incompatible_shaderpack")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }
        }
    }
}
