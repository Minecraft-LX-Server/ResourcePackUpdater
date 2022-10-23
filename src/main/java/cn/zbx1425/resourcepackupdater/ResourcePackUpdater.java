package cn.zbx1425.resourcepackupdater;

import cn.zbx1425.resourcepackupdater.gui.GlHelper;
import cn.zbx1425.resourcepackupdater.gui.GlProgressScreen;
import cn.zbx1425.resourcepackupdater.io.Dispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ResourcePackUpdater implements PreLaunchEntrypoint {

    public static final String MOD_ID = "resourcepackupdater";

    public static final Logger LOGGER = LogManager.getLogger("ResourcePackUpdater");

    public static String MOD_VERSION = "";

    public static final GlProgressScreen GL_PROGRESS_SCREEN = new GlProgressScreen();

    public static final Config CONFIG = new Config();

    @Override
    public void onPreLaunch() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get()
                .getMetadata().getVersion().getFriendlyString();
        try {
            CONFIG.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dispatchSyncWork() {
        GlHelper.initGlStates();

        Dispatcher syncDispatcher = new Dispatcher();
        ResourcePackUpdater.GL_PROGRESS_SCREEN.reset();
        try {
            syncDispatcher.runSync(ResourcePackUpdater.CONFIG.getPackBaseDir(), ResourcePackUpdater.CONFIG.activeSource, ResourcePackUpdater.GL_PROGRESS_SCREEN);

            if (ResourcePackUpdater.CONFIG.pauseWhenSuccess || ResourcePackUpdater.GL_PROGRESS_SCREEN.hasException()) {
                while (ResourcePackUpdater.GL_PROGRESS_SCREEN.pause(true)) {
                    Thread.sleep(50);
                }
            }

            Minecraft.getInstance().options.save();
        } catch (Exception ignored) {

        }
        GlHelper.resetGlStates();
    }

    public static void modifyPackList() {
        Options options = Minecraft.getInstance().options;
        String expectedEntry = "file/" + ResourcePackUpdater.CONFIG.localPackName;
        options.resourcePacks.remove(expectedEntry);
        if (!options.resourcePacks.contains("vanilla")) {
            options.resourcePacks.add("vanilla");
        }
        if (!options.resourcePacks.contains("Fabric Mods")) {
            options.resourcePacks.add("Fabric Mods");
        }
        options.resourcePacks.add(expectedEntry);
        options.incompatibleResourcePacks.remove(expectedEntry);

        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        repository.reload();
        options.loadSelectedResourcePacks(repository);
    }
}