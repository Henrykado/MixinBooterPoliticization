package zone.rong.mixinbooter;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LateMixinRedirector {
    public static void redirectLateMixins(ASMDataTable asmDataTable, ModClassLoader modClassLoader, Loader loader) throws Throwable {
        // Add mods into the delegated ModClassLoader
        // Will crash if this is not added
        for (ModContainer container : loader.getActiveModList()) {
            modClassLoader.addFile(container.getSource());
        }

        // Gather ILateMixinLoaders
        Set<ASMDataTable.ASMData> interfaceData = asmDataTable.getAll(ILateMixinLoader.class.getName().replace('.', '/'));
        Set<ILateMixinLoader> lateLoaders = new HashSet<>();

        // Instantiate all @MixinLoader annotated classes
        Set<ASMDataTable.ASMData> annotatedData = asmDataTable.getAll(MixinLoader.class.getName());

        if (!annotatedData.isEmpty()) {
            for (ASMDataTable.ASMData annotated : annotatedData) {
                try {
                    Class<?> clazz = Class.forName(annotated.getClassName());
                    MixinBooterPlugin.logInfo("Loading annotated late loader [%s] for its mixins.", clazz.getName());
                    Object instance = clazz.newInstance();
                    if (instance instanceof ILateMixinLoader) {
                        lateLoaders.add((ILateMixinLoader) instance);
                    }
                } catch (Throwable t) {
                    throw new RuntimeException("Unexpected error.", t);
                }
            }
        }

        // Instantiate all ILateMixinLoader implemented classes
        if (!interfaceData.isEmpty()) {
            for (ASMDataTable.ASMData itf : interfaceData) {
                try {
                    Class<?> clazz = Class.forName(itf.getClassName().replace('/', '.'));
                    MixinBooterPlugin.logInfo("Loading late loader [%s] for its mixins.", clazz.getName());
                    lateLoaders.add((ILateMixinLoader) clazz.newInstance());
                } catch (Throwable t) {
                    throw new RuntimeException("Unexpected error.", t);
                }
            }

            // Gather loaded mods for context
            Collection<String> presentMods = loader.getActiveModList().stream().map(ModContainer::getModId).collect(Collectors.toSet());

            for (ILateMixinLoader lateLoader : lateLoaders) {
                try {
                    for (String mixinConfig : lateLoader.getMixinConfigs()) {
                        Context context = new Context(mixinConfig, presentMods);
                        if (lateLoader.shouldMixinConfigQueue(context)) {
                            MixinBooterPlugin.logInfo("Redirecting [%s] mixin configuration.", mixinConfig);
                            FermiumRegistryAPI.enqueueMixin(true, mixinConfig);
                        }
                    }
                } catch (Throwable t) {
                    MixinBooterPlugin.logError("Failed to execute late loader [%s].", t, lateLoader.getClass().getName());
                }
            }
        }
    }
}

/*
@Mixin(value = Loader.class, remap = false)
public class LateMixinRedirector {
    @Inject(method = "loadMds", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/config/ConfigManager;loadData(Lnet/minecraftforge/fml/common/discovery/ASMDataTable;)V"))
    private void beforeConstructing(List<String> injectedModContainers) {
        MixinBooterPlugin.logInfo("test0");

        Loader self = (Loader) (Object) this;
*/