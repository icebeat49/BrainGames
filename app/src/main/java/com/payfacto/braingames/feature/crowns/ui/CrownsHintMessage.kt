package com.payfacto.braingames.feature.crowns.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.payfacto.braingames.R
import com.payfacto.braingames.feature.crowns.engine.CrownsHint
import com.payfacto.braingames.feature.crowns.engine.Area
import com.payfacto.braingames.feature.crowns.engine.AreaType
import com.payfacto.braingames.feature.crowns.engine.SolverStep
import com.payfacto.braingames.feature.crowns.engine.Technique

/**
 * One localized sentence for a hint. Rows and columns are numbered from 1; regions are
 * "the highlighted region" because the grid highlights them.
 */
@Composable
fun crownsHintMessage(hint: CrownsHint): String = when (hint) {
    is CrownsHint.WrongMark -> stringResource(
        if (hint.mark == com.payfacto.braingames.feature.crowns.model.CellMark.CROWN) {
            R.string.crowns_hint_wrong_crown
        } else {
            R.string.crowns_hint_wrong_cross
        }
    )
    is CrownsHint.Reveal -> stringResource(R.string.crowns_hint_reveal)
    is CrownsHint.Deduction -> deductionMessage(hint.step)
    CrownsHint.Solved -> ""
}

@Composable
private fun deductionMessage(step: SolverStep): String {
    val source = step.sources.first()
    return when (step.technique) {
        Technique.SINGLE_CANDIDATE -> when (source.type) {
            AreaType.ROW -> stringResource(R.string.crowns_hint_single_row, source.number)
            AreaType.COLUMN -> stringResource(R.string.crowns_hint_single_column, source.number)
            AreaType.REGION -> stringResource(R.string.crowns_hint_single_region)
        }
        Technique.CONFINED_AREA -> {
            val target = step.targets.first()
            when {
                target.type == AreaType.ROW -> stringResource(R.string.crowns_hint_confined_region_row, target.number)
                target.type == AreaType.COLUMN -> stringResource(R.string.crowns_hint_confined_region_column, target.number)
                source.type == AreaType.ROW -> stringResource(R.string.crowns_hint_confined_row_region, source.number)
                else -> stringResource(R.string.crowns_hint_confined_column_region, source.number)
            }
        }
        Technique.NEIGHBOUR_ELIMINATION -> when (source.type) {
            AreaType.ROW -> stringResource(R.string.crowns_hint_neighbour_row, source.number)
            AreaType.COLUMN -> stringResource(R.string.crowns_hint_neighbour_column, source.number)
            AreaType.REGION -> stringResource(R.string.crowns_hint_neighbour_region)
        }
        Technique.GROUP -> {
            val count = step.sources.size
            when {
                step.targets.first().type == AreaType.ROW ->
                    stringResource(R.string.crowns_hint_group_regions_rows, count, step.targets.numbers())
                step.targets.first().type == AreaType.COLUMN ->
                    stringResource(R.string.crowns_hint_group_regions_columns, count, step.targets.numbers())
                source.type == AreaType.ROW ->
                    stringResource(R.string.crowns_hint_group_rows_regions, count, step.sources.numbers())
                else ->
                    stringResource(R.string.crowns_hint_group_columns_regions, count, step.sources.numbers())
            }
        }
    }
}

private val Area.number: Int get() = index + 1

private fun List<Area>.numbers(): String = sortedBy { it.index }.joinToString(", ") { it.number.toString() }
