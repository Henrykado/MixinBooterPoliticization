package zone.rong.mixinbooter.util;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;

import java.util.List;

// Reduced to just 1.12.2
public class Environment {

    private static final boolean inDev = $inDev();
    private static final String side;

    /**
     * @return current Minecraft version
     */
    public static String minecraftVersion() {
        return "1.12.2";
    }

    /**
     * @return if the current environment is in dev
     */
    public static boolean inDev() {
        return inDev;
    }

    /**
     * @return current physical side
     */
    public static String side() {
        return side;
    }

    static {
        List<ITweaker> tweaks = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        side = tweaks.get(0).getClass().getName().endsWith("FMLServerTweaker") ? "SERVER" : "CLIENT";
    }

    private static boolean $inDev() {
        return System.getProperty("sun.java.command", "").contains("GradleStart") || System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp") != null;
    }

}
