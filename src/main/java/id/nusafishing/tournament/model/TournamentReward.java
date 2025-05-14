package id.nusafishing.tournament.model;

import java.util.ArrayList;
import java.util.List;

public class TournamentReward {
    private final double money;
    private final int exp;
    private final List<String> commands;
    
    public TournamentReward(double money, int exp, List<String> commands) {
        this.money = money;
        this.exp = exp;
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }
    
    public double getMoney() {
        return money;
    }
    
    public int getExp() {
        return exp;
    }
    
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }
}