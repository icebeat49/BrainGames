package com.payfacto.braingames.feature.kakuro.data

import android.content.Context
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import com.payfacto.braingames.feature.kakuro.model.KakuroCell
import org.json.JSONArray
import org.json.JSONObject

object KakuroPuzzleBank {

    fun load(context: Context, difficulty: Difficulty): KakuroBoard {
        val rawId = when (difficulty) {
            Difficulty.VERY_EASY -> com.payfacto.braingames.R.raw.kakuro_very_easy
            Difficulty.EASY      -> com.payfacto.braingames.R.raw.kakuro_easy
            Difficulty.MEDIUM    -> com.payfacto.braingames.R.raw.kakuro_medium
            Difficulty.HARD      -> com.payfacto.braingames.R.raw.kakuro_hard
            Difficulty.EXPERT    -> com.payfacto.braingames.R.raw.kakuro_expert
        }
        val json = context.resources.openRawResource(rawId).bufferedReader().readText()
        val puzzles = JSONArray(json)
        val puzzle = puzzles.getJSONObject((0 until puzzles.length()).random())
        return parsePuzzle(puzzle)
    }

    private fun parsePuzzle(json: JSONObject): KakuroBoard {
        val rows = json.getInt("rows")
        val cols = json.getInt("cols")
        val cellArray = json.getJSONArray("cells")

        val grid: MutableList<MutableList<KakuroCell>> = MutableList(rows) {
            MutableList(cols) { KakuroCell.Black }
        }

        for (i in 0 until cellArray.length()) {
            val cellJson = cellArray.getJSONObject(i)
            val r = cellJson.getInt("r")
            val c = cellJson.getInt("c")
            grid[r][c] = when (cellJson.getString("type")) {
                "CLUE"  -> KakuroCell.Clue(
                    downSum = cellJson.optInt("down", 0),
                    acrossSum = cellJson.optInt("across", 0)
                )
                "INPUT" -> KakuroCell.Input(solution = cellJson.getInt("solution"))
                else    -> KakuroCell.Black
            }
        }
        return KakuroBoard(rows = rows, cols = cols, cells = grid)
    }
}
