package com.example.disasters.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.net.URI;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = "naturaldisasters")
public class AntiRepostHandler {

    private static final String OFFICIAL_JAR_NAME = "naturaldisasters-26.3-1.0.0.jar";
    private static final String CURSEFORGE_URL = "https://" + "://curseforge.com";

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!FMLEnvironment.production) return;

        try {
            Player player = event.getEntity();
            Path jarPath = Path.of(AntiRepostHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String actualFileName = jarPath.getFileName().toString();

            if (!actualFileName.equals(OFFICIAL_JAR_NAME)) {
                Component msg = Component.literal("[CẢNH BÁO / WARNING] ")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        .append(Component.literal("Bạn đang dùng bản mod re-up trái phép. Tải bản gốc tại CurseForge: ")
                                .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("[Tải bản chính chủ]")
                                .withStyle(Style.EMPTY
                                        .withColor(ChatFormatting.AQUA)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(CURSEFORGE_URL)))));

                player.sendSystemMessage(msg);
            }
        } catch (Exception ignored) {
        }
    }
}
