package com.example.streamapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.streamapp.databinding.ActivityMainBinding;

public class TennisScoreLogic {
    private final Context context;
    private final ActivityMainBinding binding;
    private int pointA = 0, pointB = 0, currentSet = 1, setsWonA = 0, setsWonB = 0, noOfSets = 3, setsToWin = 2;
    private final int[] gamesA = {0, 0, 0, 0, 0, 0, 0};
    private final int[] gamesB = {0, 0, 0, 0, 0, 0, 0};
    private boolean matchFinished = false, serveA = true;

    public TennisScoreLogic(Context context, ActivityMainBinding binding) {
        this.context = context;
        this.binding = binding;

        initButtonClickListeners();
        updateGameScore();
        updateSetScore();
    }

    private void initButtonClickListeners() {

        //Buttons that increase or decrease the Game scores

        binding.teamAPlus.setOnClickListener(v -> {addPointToPlayerA();});

        binding.teamAMinus.setOnClickListener(v -> {removePointFromPlayerA();});

        binding.teamBPlus.setOnClickListener(v -> {addPointToPlayerB();});

        binding.teamBMinus.setOnClickListener(v -> {removePointFromPlayerB();});

        //Buttons that increase or decrease the Set scores

        binding.addSetTma.setOnClickListener(v -> {increaseGamePlayerA();});

        binding.removeSetTma.setOnClickListener(v -> {decreaseGamePlayerA();});

        binding.addSetTmb.setOnClickListener(v -> {increaseGamePlayerB();});

        binding.removeSetTmb.setOnClickListener(v -> {decreaseGamePlayerB();});

        binding.set3.setOnClickListener(v->{
            noOfSets =3;
            setsToWin= 2;
            Toast.makeText(context, "3 sets selected", Toast.LENGTH_SHORT).show();});

        binding.set5.setOnClickListener(v->{
            noOfSets =5;
            setsToWin= 3;
            Toast.makeText(context, "5 sets selected", Toast.LENGTH_SHORT).show();});

        binding.set7.setOnClickListener(v->{
            noOfSets =7;
            setsToWin= 4;
            Toast.makeText(context, "7 sets selected", Toast.LENGTH_SHORT).show();});
    }

    private void addPointToPlayerA() {
        if (matchFinished) return;

        pointA++;
        evaluateGameWinner();
        updateGameScore();
    }

    private void addPointToPlayerB() {

        if (matchFinished) return;

        pointB++;
        evaluateGameWinner();
        updateGameScore();
    }

    private void removePointFromPlayerA() {

        if (matchFinished) return;

        if (pointA > 0) {
            pointA--;
        }
        updateGameScore();
    }

    private void removePointFromPlayerB() {

        if (matchFinished) return;

        if (pointB > 0) {
            pointB--;
        }
        updateGameScore();
    }

    private void evaluateGameWinner() {

        if (gamesA[currentSet - 1] == 6 && gamesB[currentSet - 1] == 6) {

            if (pointA >= 7 && pointA - pointB >= 2) {
                gamesA[currentSet - 1]++;
                resetPoints();
                changeServe();
                evaluateSetWinner();
            } else if (pointB >= 7 && pointB - pointA >= 2) {
                gamesB[currentSet - 1]++;
                resetPoints();
                changeServe();
                evaluateSetWinner();
            }

            return;
        }

        if (pointA >= 4 && pointA - pointB >= 2) {

            gamesA[currentSet - 1]++;
            resetPoints();
            changeServe();
            evaluateSetWinner();
        }
        else if (pointB >= 4 && pointB - pointA >= 2) {

            gamesB[currentSet - 1]++;
            resetPoints();
            changeServe();
            evaluateSetWinner();
        }
    }

    private void evaluateSetWinner() {

        int a = gamesA[currentSet - 1];
        int b = gamesB[currentSet - 1];

        boolean playerAWon = (a >= 6 && a - b >= 2) || (a == 7);

        boolean playerBWon = (b >= 6 && b - a >= 2) || (b == 7);

        updateSetScore();

        if (playerAWon) {

            setsWonA++;
            changeScoreColor(currentSet,"A");
            resetSetPoints();
            moveToNextSet();
        } else if (playerBWon) {

            setsWonB++;
            changeScoreColor(currentSet,"B");
            resetSetPoints();
            moveToNextSet();
        }

        if (setsWonA == setsToWin || setsWonB == setsToWin) {
            matchFinished = true;
        }
    }

    private void moveToNextSet() {
        if (currentSet == 1) {
            binding.set2Card.setVisibility(View.VISIBLE);
            currentSet = 2;
        }
        else if (currentSet == 2) {
            binding.set3Card.setVisibility(View.VISIBLE);
            currentSet = 3;
        }
        else if(currentSet == 3 && noOfSets >= 5) {
            binding.set4Card.setVisibility(View.VISIBLE);
            currentSet = 4;
        }
        else if(currentSet == 4 && noOfSets >= 5) {
            binding.set5Card.setVisibility(View.VISIBLE);
            currentSet = 5;
        }
        else if(currentSet == 5 && noOfSets == 7) {
            binding.set6Card.setVisibility(View.VISIBLE);
            currentSet = 6;
        }
        else if(currentSet == 6 && noOfSets == 7) {
            binding.set7Card.setVisibility(View.VISIBLE);
            currentSet = 7;
        }
    }


    private void increaseGamePlayerA() {

        if (matchFinished) return;

        gamesA[currentSet - 1]++;
        evaluateSetWinner();
        updateSetScore();
        changeServe();
    }

    private void decreaseGamePlayerA() {

        if (gamesA[currentSet - 1] > 0) {
            gamesA[currentSet - 1]--;
        }
        updateSetScore();
    }

    private void increaseGamePlayerB() {
        if (matchFinished) return;

        gamesB[currentSet - 1]++;
        evaluateSetWinner();
        updateSetScore();
        changeServe();
    }

    private void decreaseGamePlayerB() {

        if (gamesB[currentSet - 1] > 0) {
            gamesB[currentSet - 1]--;
        }
        updateSetScore();
    }

    private void updateGameScore() {

        binding.tmaScore.setText(getTennisPoint(pointA, pointB));

        binding.tmbScore.setText(getTennisPoint(pointB, pointA));

        binding.currentSetAScore.setText(getTennisPoint(pointA, pointB));

        binding.currentSetBScore.setText(getTennisPoint(pointB, pointA));
    }

    private void updateSetScore() {
        switch(currentSet) {
            case 1:
                binding.set1A.setText(String.valueOf(gamesA[0]));
                binding.set1B.setText(String.valueOf(gamesB[0]));
                break;
            case 2:
                binding.set2A.setText(String.valueOf(gamesA[1]));
                binding.set2B.setText(String.valueOf(gamesB[1]));
                break;
            case 3:
                binding.set3A.setText(String.valueOf(gamesA[2]));
                binding.set3B.setText(String.valueOf(gamesB[2]));
                break;
            case 4:
                binding.set4A.setText(String.valueOf(gamesA[3]));
                binding.set4B.setText(String.valueOf(gamesB[3]));
                break;
            case 5:
                binding.set5A.setText(String.valueOf(gamesA[4]));
                binding.set5B.setText(String.valueOf(gamesB[4]));
                break;
            case 6:
                binding.set6A.setText(String.valueOf(gamesA[5]));
                binding.set6B.setText(String.valueOf(gamesB[5]));
                break;
            case 7:
                binding.set7A.setText(String.valueOf(gamesA[6]));
                binding.set7B.setText(String.valueOf(gamesB[6]));
                break;
        }

        binding.setScoreTma.setText(String.valueOf(gamesA[currentSet - 1]));

        binding.setScoreTmb.setText(String.valueOf(gamesB[currentSet - 1]));
    }

    private String getTennisPoint(int player, int opponent) {

        if (gamesA[currentSet - 1] == 6 && gamesB[currentSet - 1] == 6) {
            return String.valueOf(player);
        }

        if (player >= 3 && opponent >= 3) {

            if (player == opponent) {
                return "D";
            }

            if (player > opponent) {
                return "A";
            }
        }

        switch (player) {

            case 0:
                return "0";

            case 1:
                return "15";

            case 2:
                return "30";

            case 3:
                return "40";
        }
        return "40";
    }

    private void resetPoints() {
        pointA = 0;
        pointB = 0;
    }

    private void resetSetPoints()
    {
        binding.setScoreTma.setText(String.valueOf(0));
        binding.setScoreTmb.setText(String.valueOf(0));
    }

    private void changeServe()
    {
        if(serveA){
            binding.plAServe.setImageResource(R.drawable.grey_arrow);
            binding.plBServe.setImageResource(R.drawable.green_arrow);
            serveA = false;
        }
        else if(!serveA){
            binding.plAServe.setImageResource(R.drawable.green_arrow);
            binding.plBServe.setImageResource(R.drawable.grey_arrow);
            serveA = true;
        }

    }
    private void changeScoreColor(int currentSet, String player)
    {
        TextView[] setA = {binding.set1A, binding.set2A, binding.set3A,binding.set4A, binding.set5A, binding.set6A,binding.set7A};
        TextView[] setB = {binding.set1B, binding.set2B, binding.set3B,binding.set4B, binding.set5B, binding.set6B,binding.set7B};

        int index = currentSet - 1;

        if(player == "A"){
            setA[index].setTextColor(ContextCompat.getColor(context, R.color.selected_green));
        }

        if(player == "B"){
            setB[index].setTextColor(ContextCompat.getColor(context, R.color.selected_green));
        }
    }
}