package qouteall.imm_ptl.core.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;

public class PehkuiInterface {
    
    public static class Invoker {
        public boolean isPehkuiPresent() {
            return false;
        }
        
        public void onClientPlayerTeleported(Portal portal) {
            showMissingPehkui(portal);
        }
        
        public void onServerEntityTeleported(Entity entity, Portal portal) {
        
        }
        
        public float getBaseScale(Entity entity) {
            return getBaseScale(entity, 1.0f);
        }
        
        public float getBaseScale(Entity entity, float partialTick) {
            return 1.0f;
        }
        
        public void setBaseScale(Entity entity, float scale) {
            
        }
        
        public float computeThirdPersonScale(Entity entity, float partialTick) {
            return 1.0f;
        }
        
        public float computeBlockReachScale(Entity entity) {
            return computeBlockReachScale(entity, 1.0f);
        }
        
        public float computeBlockReachScale(Entity entity, float partialTick) {
            return 1.0f;
        }
        
        public float computeMotionScale(Entity entity) {
            return computeMotionScale(entity, 1.0f);
        }
        
        public float computeMotionScale(Entity entity, float partialTick) {
            return 1.0f;
        }
    }
    
    public static Invoker invoker = new Invoker();
    
    private static boolean messageShown = false;
    
    //@OnlyIn(Dist.CLIENT)
    private static void showMissingPehkui(Portal portal) {
        if (portal.hasScaling() && portal.isTeleportChangesScale()) {
            if (!messageShown) {
                messageShown = true;
                Minecraft.getInstance().gui.getChat().addMessage(
                    Component.translatable("imm_ptl.needs_pehkui")
                        .append(McHelper.getLinkText("https://modrinth.com/mod/pehkui"))
                );
            }
        }
    }
    
}
