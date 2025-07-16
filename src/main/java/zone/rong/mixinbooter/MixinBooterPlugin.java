package zone.rong.mixinbooter;

import fermiumbooter.FermiumRegistryAPI;
import fermiumbooter.util.FermiumJarScanner;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

@IFMLLoadingPlugin.Name("MixinBooter")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE + 1)
public final class MixinBooterPlugin implements IFMLLoadingPlugin {

    public static final Logger LOGGER = LogManager.getLogger("MixinBooter");

    static String getMinecraftVersion() {
        return (String) FMLInjectionData.data()[4];
    }

    public MixinBooterPlugin() {}

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"zone.rong.mixinbooter.MixinBooterClassTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return "zone.rong.mixinbooter.MixinBooterModContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Object coremodList = data.get("coremodList");
        LOGGER.info("injecting data");
        if (coremodList instanceof List) {
            this.loadMixinLoaders((List) coremodList);
        } else {
            throw new RuntimeException("Blackboard property 'coremodList' must be of type List, early loaders were not able to be gathered");
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    private void loadMixinLoaders(List coremodList) {
        Field fmlPluginWrapper$coreModInstance = null;

        FermiumRegistryAPI.isModPresent("foo"); // run just to ensure earlyModIDs isn't null

        Set<String> presentMods;
        try {
            Field field = FermiumJarScanner.class.getDeclaredField("earlyModIDs");
            field.setAccessible(true);

            presentMods = (Set<String>) field.get(null); // For hijackers
        } catch (Throwable t) {
            logError("Failed to load early mixins and hijackers.", t);
            return;
        }

        for (Object coremod : coremodList) {
            try {
                if (fmlPluginWrapper$coreModInstance == null) {
                    fmlPluginWrapper$coreModInstance = coremod.getClass().getField("coreModInstance");
                    fmlPluginWrapper$coreModInstance.setAccessible(true);
                }
                Object theMod = fmlPluginWrapper$coreModInstance.get(coremod);
                if (theMod instanceof IMixinConfigHijacker) {
                    IMixinConfigHijacker interceptor = (IMixinConfigHijacker) theMod;
                    logInfo("Redirecting config hijacker %s.", interceptor.getClass().getName());
                    for (String hijacked : interceptor.getHijackedMixinConfigs(new Context(null, presentMods))) {
                        FermiumRegistryAPI.removeMixin(hijacked);
                    }
                }
                if (theMod instanceof IEarlyMixinLoader) {
                    IEarlyMixinLoader earlyMixinLoader = (IEarlyMixinLoader) theMod;
                    for (String mixinConfig : earlyMixinLoader.getMixinConfigs()) {
                        if (earlyMixinLoader.shouldMixinConfigQueue(new Context(mixinConfig, presentMods))) {
                            logInfo("Redirecting [%s] mixin configuration.", mixinConfig);
                            FermiumRegistryAPI.enqueueMixin(false, mixinConfig);
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Unexpected error", t);
            }
        }
    }

    public static void logInfo(String message, Object... params) {
        LOGGER.info(String.format(message, params));
    }

    public static void logError(String message, Throwable t, Object... params) {
        LOGGER.error(String.format(message, params), t);
    }

    public static void logDebug(String message, Object... params) {
        LOGGER.debug(String.format(message, params));
    }
}
