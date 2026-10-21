package zone.rong.mixinbooter.service;

import fermiumbooter.FermiumRegistryAPI;
import fermiumbooter.util.FermiumJarScanner;
import fermiumbooter.util.ModInfo;
import zone.rong.mixinbooter.MixinBooterPlugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Mirror of MixinBooter 11's public mod discovery API, backed by FermiumBooter's jar scanner.
 */
public final class ModDiscoverer {

    private static Set<String> presentMods;

    private ModDiscoverer() { }

    /**
     * Returns whether any discovered jar declares the given mod id.
     *
     * @param modId the mod id to test
     * @return {@code true} if at least one scanned jar declares the id
     */
    public static boolean isModPresent(String modId) {
        return FermiumRegistryAPI.isModPresent(modId);
    }

    /**
     * Returns the ids of every mod discovered across all scanned jars.
     *
     * @return an unmodifiable view of all present mod ids
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getPresentMods() {
        if (presentMods == null) {
            FermiumJarScanner.handleCaching(); // starts the fermium jar scan if it hasn't happened yet
            try {
                Field field = FermiumJarScanner.class.getDeclaredField("presentMods");
                field.setAccessible(true);
                presentMods = Collections.unmodifiableSet(((Map<String, ModInfo>) field.get(null)).keySet());
            } catch (Throwable t) {
                MixinBooterPlugin.logError("Failed to gather present mods from FermiumBooter.", t);
                return Collections.emptySet();
            }
        }
        return presentMods;
    }

}
