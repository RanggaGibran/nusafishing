package id.nusafishing.tournament.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tournament {
    private final String id;
    private final String name;
    private final String description;
    private final TournamentType type;
    private final int durationMinutes;
    
    // Scheduling
    private int hourOfDay = 12; // Default: 12 PM
    private int minuteOfHour = 0;
    private List<String> daysOfWeek = new ArrayList<>();
    private boolean autoStart = true;
    
    // Runtime state
    private long startTime = 0;
    private long endTime = 0;
    
    // Target species (for SPECIFIC_SPECIES type)
    private List<String> targetSpecies = new ArrayList<>();
    
    // Rewards
    private final Map<String, TournamentReward> rewards = new HashMap<>();
    
    public Tournament(String id, String name, String description, TournamentType type, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.durationMinutes = durationMinutes;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public TournamentType getType() {
        return type;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public List<String> getTargetSpecies() {
        return targetSpecies;
    }
    
    public void setTargetSpecies(List<String> targetSpecies) {
        this.targetSpecies = targetSpecies;
    }
    
    public int getHourOfDay() {
        return hourOfDay;
    }
    
    public int getMinuteOfHour() {
        return minuteOfHour;
    }
    
    public void setScheduleTime(int hourOfDay, int minuteOfHour) {
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
    }
    
    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }
    
    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
    
    public boolean isAutoStart() {
        return autoStart;
    }
    
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
    
    // Reward methods
    public void addReward(String place, double money, int exp, List<String> commands) {
        rewards.put(place, new TournamentReward(money, exp, commands));
    }
    
    public double getRewardMoney(String place) {
        TournamentReward reward = rewards.get(place);
        return reward != null ? reward.getMoney() : 0;
    }
    
    public int getRewardExp(String place) {
        TournamentReward reward = rewards.get(place);
        return reward != null ? reward.getExp() : 0;
    }
    
    public List<String> getRewardCommands(String place) {
        TournamentReward reward = rewards.get(place);
        return reward != null ? reward.getCommands() : new ArrayList<>();
    }
}