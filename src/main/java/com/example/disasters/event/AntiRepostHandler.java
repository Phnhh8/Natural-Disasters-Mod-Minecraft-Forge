package com.example.disasters.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = "naturaldisasters")
public class AntiRepostHandler {

    // Tên file .jar gốc chuẩn xác mà bạn upload lên CurseForge
    private static final String OFFICIAL_JAR_NAME = "naturaldisasters-1.0.0.jar";
    private static final String CURSEFORGE_URL = "https://curseforge.com";

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!FMLEnvironment.production) {
            return;
        }

        PlayerEntity player = event.getPlayer();
        Path jarPath = ModList.get().getModFileById("naturaldisasters").getFile().getFilePath();
        String actualFileName = jarPath.getFileName().toString();

        if (!actualFileName.equals(OFFICIAL_JAR_NAME)) {
            IFormattableTextComponent warningTitle = new StringTextComponent("[CẢNH BÁO / WARNING] ")
                    .withStyle(TextFormatting.RED, TextFormatting.BOLD);

            IFormattableTextComponent warningText = new StringTextComponent(
                    "Bạn đang sử dụng bản mod bị đăng lại trái phép (re-up) từ trang web không chính chủ (như 9Minecraft). " +
                    "Để đảm bảo an toàn và ủng hộ tác giả Phnhh_8, hãy tải bản gốc tại CurseForge: "
            ).withStyle(TextFormatting.YELLOW);

            IFormattableTextComponent clickableLink = new StringTextComponent("[Bấm vào đây để tải bản chính chủ]")
                    .withStyle(Style.EMPTY
                            .withColor(TextFormatting.AQUA)
                            .setUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, CURSEFORGE_URL))
                    );

            player.sendMessage(warningTitle.append(warningText).append(clickableLink), Util.NIL_UUID);
        }
    }
}
