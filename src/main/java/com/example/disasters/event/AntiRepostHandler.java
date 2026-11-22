package com.example.disasters.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = "disasters") // Kiểm tra lại xem MOD_ID trong NaturalDisastersMod.java có phải là "disasters" không nhé!
public class AntiRepostHandler {

    // Tên file .jar gốc chuẩn xác mà bạn upload lên CurseForge
    private static final String OFFICIAL_JAR_NAME = "naturaldisasters-1.20.1-1.0.0.jar";
    private static final String CURSEFORGE_URL = "https://curseforge.com";

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!FMLEnvironment.production) {
            return;
        }

        Player player = event.getEntity();
        
        // Thay "disasters" ở dòng dưới bằng đúng MOD_ID của bạn nếu khác
        Path jarPath = ModList.get().getModFileById("disasters").getFile().getFilePath();
        String actualFileName = jarPath.getFileName().toString();

        if (!actualFileName.equals(OFFICIAL_JAR_NAME)) {
            Component warningTitle = Component.literal("[CẢNH BÁO / WARNING] ")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

            Component warningText = Component.literal(
                    "Bạn đang sử dụng bản mod bị đăng lại trái phép (re-up) từ trang web không chính chủ (như 9Minecraft). " +
                    "Để đảm bảo an toàn và ủng hộ tác giả Phnhh_8, hãy tải bản gốc tại CurseForge: "
            ).withStyle(ChatFormatting.YELLOW);

            Component clickableLink = Component.literal("[Bấm vào đây để tải bản chính chủ]")
                    .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, CURSEFORGE_URL))
                    );

            player.sendSystemMessage(warningTitle.copy().append(warningText).append(clickableLink));
        }
    }
}
