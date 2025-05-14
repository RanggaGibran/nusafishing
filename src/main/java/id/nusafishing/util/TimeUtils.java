package id.nusafishing.util;

public class TimeUtils {
    /**
     * Format waktu dalam detik menjadi format jam:menit:detik
     * @param seconds Waktu dalam detik
     * @return String terformat
     */
    public static String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
    
    /**
     * Format waktu dalam milidetik menjadi format jam:menit:detik
     * @param millis Waktu dalam milidetik
     * @return String terformat
     */
    public static String formatTimeMillis(long millis) {
        return formatTime((int)(millis / 1000));
    }
}