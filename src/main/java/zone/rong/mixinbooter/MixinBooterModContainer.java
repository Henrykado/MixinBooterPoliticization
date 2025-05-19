package zone.rong.mixinbooter;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

import java.util.Collections;
import java.util.Set;

public class MixinBooterModContainer extends DummyModContainer {

    public MixinBooterModContainer() {
        super(new ModMetadata());
        MixinBooterPlugin.LOGGER.info("Initializing MixinBooter's Mod Container.");
        ModMetadata meta = this.getMetadata();
        meta.modId = Tags.MOD_ID;
        meta.name = Tags.MOD_NAME;
        meta.description = "A mod that redirects mixin configurations added through MixinBooter, so they are loaded by FermiumBooter instead.";
        meta.credits = "Thanks to LegacyModdingMC + Fabric for providing the initial mixin fork.";
        meta.version = Tags.VERSION;
        meta.logoFile = "/icon.png";
        meta.authorList.add("Rongmario");
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Override
    public Set<ArtifactVersion> getRequirements() {
        if ("1.12.2".equals(MixinBooterPlugin.getMinecraftVersion())) {
            return Collections.singleton(new DefaultArtifactVersion("fermiumbooter", true));
        }
        return Collections.emptySet();
    }
}
