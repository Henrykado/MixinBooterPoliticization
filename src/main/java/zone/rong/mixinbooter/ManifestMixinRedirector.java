package zone.rong.mixinbooter;

import fermiumbooter.FermiumRegistryAPI;
import fermiumbooter.util.FermiumJarScanner;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Constants.ManifestAttributes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * MixinBooter 11 reads the {@code MixinConfigs} and {@code MixinConnector} manifest entries of every mod jar.
 * Mixin itself only reads them from jars declaring {@code MixinTweaker} as their {@code TweakClass},
 * so the remaining jars are handled here. Their configs are enqueued as early FermiumBooter mixins,
 * which can target mod classes since FermiumBooter 1.6.0 adds mod jars to the classpath early.
 */
public class ManifestMixinRedirector {

    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";

    public static void redirectManifestMixins() {
        FermiumJarScanner.handleCaching(); // FermiumBooter adds the mod jars to the classpath while scanning

        Set<File> jars = new LinkedHashSet<>();
        for (URL url : Launch.classLoader.getURLs()) {
            try {
                jars.add(new File(url.toURI()));
            } catch (Exception ignored) { }
        }

        Set<String> scanned = new HashSet<>();
        List<String> connectors = new ArrayList<>();
        for (File jar : jars) {
            if (!jar.isFile() || !jar.getName().endsWith(".jar")) {
                continue;
            }
            try (JarFile jarFile = new JarFile(jar)) {
                if (!scanned.add(jar.getCanonicalPath())) {
                    continue;
                }
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    continue;
                }
                Attributes attributes = manifest.getMainAttributes();
                // Mixin already handles these as containers
                if (MIXIN_TWEAKER.equals(attributes.getValue(ManifestAttributes.TWEAKER))) {
                    continue;
                }
                String configs = attributes.getValue(ManifestAttributes.MIXINCONFIGS);
                String connector = attributes.getValue(ManifestAttributes.MIXINCONNECTOR);
                if (configs == null && connector == null) {
                    continue;
                }
                if (configs != null) {
                    for (String mixinConfig : configs.split(",")) {
                        mixinConfig = mixinConfig.trim();
                        if (!mixinConfig.isEmpty()) {
                            MixinBooterPlugin.logInfo("Redirecting [%s] mixin configuration from the manifest of %s.", mixinConfig, jar.getName());
                            FermiumRegistryAPI.enqueueMixin(false, mixinConfig);
                        }
                    }
                }
                if (connector != null && !connector.trim().isEmpty()) {
                    connectors.add(connector.trim());
                }
            } catch (IOException e) {
                MixinBooterPlugin.logError("Failed to read the manifest of %s.", e, jar.getName());
            }
        }

        for (String connector : connectors) {
            try {
                Class<?> clazz = MixinService.getService().getClassProvider().findClass(connector, false);
                MixinBooterPlugin.logInfo("Calling mixin connector [%s].", connector);
                ((IMixinConnector) clazz.newInstance()).connect();
            } catch (Throwable t) {
                MixinBooterPlugin.logError("Failed to call mixin connector [%s].", t, connector);
            }
        }
    }

}
