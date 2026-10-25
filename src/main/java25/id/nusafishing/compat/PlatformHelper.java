package id.nusafishing.compat;

/**
 * Platform helper - Java 25 optimized version (Paper 26.2+ baseline).
 * Loaded automatically via Java Multi-Release JAR (META-INF/versions/25).
 */
public class PlatformHelper {
    public static int getJavaRelease() {
        return 25;
    }

    public static String getRuntimeStrategy() {
        return "Java 25 Optimized (Paper 26.2+ baseline)";
    }

    public static boolean isJava25Optimized() {
        return true;
    }
}
