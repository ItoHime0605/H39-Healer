package tw.h39.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-disruptive, server-side verification of actually loaded H-39 content.
 *
 * A successful Gradle build cannot prove that TaCZ imported the embedded
 * gunpack. Query the live TaCZ indexes after a server starts and report any
 * missing gun/ammo data or medical effects in the server log.
 *
 * No automated repair, item injection or world mutation takes place here.
 */
public final class H39RuntimeDiagnostics {
    private static final Logger LOG = LoggerFactory.getLogger("H39-Healer");
    private static final ResourceLocation GUN =
            ResourceLocation.fromNamespaceAndPath("h39", "h39");
    private static final ResourceLocation TUBE =
            ResourceLocation.fromNamespaceAndPath("h39", "medical_tube");
    private static final String[] TRAUMA = {
            "bleeding", "internal_bleeding", "blood_loss",
            "bone_fracture", "laceration", "burns"
    };

    private H39RuntimeDiagnostics() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(H39RuntimeDiagnostics.class);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        List<String> missing = scanMissing();

        if (missing.isEmpty()) {
            LOG.info("[H-39] 啟動檢查通過：TaCZ 槍枝、醫療藥管及六種外傷效果已註冊。");
        } else {
            LOG.warn("[H-39] 啟動檢查發現 {} 項缺漏。JAR 編譯成功不代表遊戲內容正確載入。", missing.size());
            for (String item : missing) {
                LOG.warn("[H-39] 缺少：{}", item);
            }
            LOG.warn("[H-39] 請確認內嵌槍包已匯出至 TaCZ 槍包資料夾，且已安裝 Meds and Herbs。");
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("h39")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("check").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    List<String> missing = scanMissing();
                    if (missing.isEmpty()) {
                        source.sendSuccess(() -> Component.literal(
                                "[H-39] 檢查通過：TaCZ 槍枝、醫療藥管及六種外傷效果已載入。"), false);
                        return 1;
                    }
                    source.sendFailure(Component.literal(
                            "[H-39] 找到 " + missing.size() + " 項缺少的內容："));
                    for (String item : missing) {
                        source.sendFailure(Component.literal("[H-39] 缺少：" + item));
                    }
                    return 0;
                })));
    }

    private static List<String> scanMissing() {
        List<String> missing = new ArrayList<>();
        if (TimelessAPI.getCommonGunIndex(GUN).isEmpty()) {
            missing.add("TaCZ 槍枝：h39:h39");
        }
        if (TimelessAPI.getCommonAmmoIndex(TUBE).isEmpty()) {
            missing.add("TaCZ 彈藥：h39:medical_tube");
        }

        if (!ModList.get().isLoaded("meds_and_herbs")) {
            missing.add("必要模組：meds_and_herbs");
        } else {
            for (String id : TRAUMA) {
                ResourceLocation key =
                        ResourceLocation.fromNamespaceAndPath("meds_and_herbs", id);
                if (BuiltInRegistries.MOB_EFFECT.getOptional(key).isEmpty()) {
                    missing.add("醫療外傷效果：" + key);
                }
            }
        }

        return missing;
    }
}
