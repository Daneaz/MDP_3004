package com.example.hanwe.mdp.Configuration;

import android.graphics.Color;

public interface Config {
    String log_id = "NTU_MDP";

    int ARENA_LENGTH = 15;
    int ARENA_WIDTH  = 20;
    int ARENA_AREA   = ARENA_LENGTH * ARENA_WIDTH;

    int BORDER = Color.parseColor("#327865");
    int UNEXPLORED = Color.parseColor("#DBDADA"); //F2F1F1
    int FREE_SPACE = Color.WHITE;
    int OBSTACLE   = Color.BLACK;

    int START = Color.parseColor("#4EBB9D");
    int GOAL = Color.parseColor("#FFA638");
}