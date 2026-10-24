package id.nusafishing.compat;

/**
 * Platform helper - Base version for Java 21+ (Minecraft 1.21.x baseline).
 */
public class PlatformHelper {
    public static int getJavaRelease() {
        return 21;
    }

    public static String getRuntimeStrategy() {
        return "Java 21 Base (Spigot/Paper 1.21.x compatible)";
    }

    public static boolean isJava25Optimized() {
        return false;
    }
}
