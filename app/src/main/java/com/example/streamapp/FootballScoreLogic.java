package com.example.streamapp;

public class FootballScoreLogic {
    private int teamAScore = 0, teamBScore = 0, currentPeriod = 1;
    private final int MAX_PERIODS = 4;
    private boolean overtime = false;

    public void goalTeamA() {
        teamAScore++;
    }

    public void goalTeamB() {
        teamBScore++;
    }

    public void removeGoalTeamA() {
        if (teamAScore > 0) {
            teamAScore--;
        }
    }
    public void removeGoalTeamB() {
        if(teamBScore > 0) {
            teamBScore--;
        }
    }

    public void ownGoalTeamA() {
        teamBScore++;
    }
    public void ownGoalTeamB() {
        teamAScore++;
    }
    public void nextPeriod() {
        if(overtime) {
            return;
        }

        if(currentPeriod < MAX_PERIODS) {
            currentPeriod++;
        }
        else {
            if(teamAScore == teamBScore) {
                overtime = true;
            }
        }
    }

    public void resetMatch() {
        teamAScore = 0;
        teamBScore = 0;
        currentPeriod = 1;
        overtime = false;
    }

    public int getTeamAScore() {
        return teamAScore;
    }
    public int getTeamBScore() {
        return teamBScore;
    }
    public String getPeriodText() {

        if (overtime) {
            return "OVERTIME";
        }

        switch(currentPeriod) {
            case 1: return "1st Period";

            case 2: return "2nd Period";

            case 3: return "3rd Period";

            case 4: return "4th Period";

            default: return "Period";
        }
    }

    public boolean isMatchFinished() {
        if(overtime) {
            return teamAScore != teamBScore;
        }
        return currentPeriod == MAX_PERIODS && teamAScore != teamBScore;

    }
    public String getWinner() {
        if(teamAScore > teamBScore) {
            return "TEAM A WON";
        }
        else if(teamBScore > teamAScore) {
            return "TEAM B WON";
        }
        return "DRAW";
    }
    public boolean isOvertime() {
        return overtime;
    }

}
