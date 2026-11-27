package com.payfacto.braingames.feature.home

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.ui.GameCatalog
import kotlin.math.roundToInt

/** Slot geometry of a 2-column grid, in pixels. */
internal class GridSlots(private val cellWidth: Float, private val cellHeight: Float, private val gap: Float, private val count: Int) {
    val rows: Int get() = (count + COLUMNS - 1) / COLUMNS

    val height: Float get() = rows * cellHeight + (rows - 1).coerceAtLeast(0) * gap

    fun origin(index: Int): Offset = Offset((index % COLUMNS) * (cellWidth + gap), (index / COLUMNS) * (cellHeight + gap))

    /** The slot under [point], or null in a gap or outside the grid. */
    fun slotAt(point: Offset): Int? = (0 until count).firstOrNull { index ->
        val origin = origin(index)
        point.x in origin.x..(origin.x + cellWidth) && point.y in origin.y..(origin.y + cellHeight)
    }

    /** The slot whose centre is nearest to [point]: where a dragged card would land. */
    fun nearestSlot(point: Offset): Int = (0 until count).minBy { index ->
        (origin(index) + Offset(cellWidth / 2, cellHeight / 2) - point).getDistanceSquared()
    }

    companion object {
        const val COLUMNS = 2
    }
}

/** Moves [item] to [to], shifting the others. */
internal fun <T> List<T>.moved(item: T, to: Int): List<T> {
    val from = indexOf(item)
    if (from < 0 || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * The games in two columns. Tap a card to open the game; long-press it, then drag, to move it.
 * The order is handed to [onReorder] when the card is dropped.
 *
 * All touches are handled by the grid itself, which does not move, rather than by each card:
 * a dragged card that follows the finger would otherwise see its own movement in every event.
 */
@Composable
fun GameGrid(
    games: List<GameType>,
    onOpen: (GameType) -> Unit,
    onReorder: (List<GameType>) -> Unit,
    onMove: (GameType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var order by remember(games) { mutableStateOf(games) }
    var dragged by remember { mutableStateOf<GameType?>(null) }
    var dragTopLeft by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    val currentOnReorder by rememberUpdatedState(onReorder)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val gap = with(density) { GAP.toPx() }
        val cellWidth = (constraints.maxWidth - gap) / GridSlots.COLUMNS
        val cellHeight = cellWidth * CELL_ASPECT
        val slots = GridSlots(cellWidth, cellHeight, gap, order.size)
        val currentSlots by rememberUpdatedState(slots)

        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { slots.height.toDp() })
                // Restarted when the games or the cell size change, so it never works on a stale order.
                .pointerInput(games, cellWidth) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pressed = currentSlots.slotAt(down.position)?.let { order[it] } ?: return@awaitEachGesture
                        val longPress = awaitLongPressOrCancellation(down.id)
                        if (longPress == null) {
                            // Released before the long press: a tap, if it stayed put on the card.
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            val stayed = up != null && !up.pressed && !up.isConsumed &&
                                (up.position - down.position).getDistance() < viewConfiguration.touchSlop
                            if (stayed) {
                                up!!.consume()
                                currentOnOpen(pressed)
                            }
                            return@awaitEachGesture
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragged = pressed
                        dragTopLeft = currentSlots.origin(order.indexOf(pressed))
                        val grabOffset = longPress.position - dragTopLeft
                        drag(longPress.id) { change ->
                            change.consume()
                            dragTopLeft = change.position - grabOffset
                            val centre = dragTopLeft + Offset(cellWidth / 2, cellHeight / 2)
                            order = order.moved(pressed, currentSlots.nearestSlot(centre))
                        }
                        dragged = null
                        currentOnReorder(order)
                    }
                }
        ) {
            order.forEachIndexed { index, game ->
                key(game) {
                    val isDragged = game == dragged
                    val slot by animateIntOffsetAsState(slots.origin(index).round(), label = "slot")
                    val position = if (isDragged) dragTopLeft.round() else slot
                    GameTile(
                        game = game,
                        index = index,
                        count = order.size,
                        lifted = isDragged,
                        onOpen = { onOpen(game) },
                        onMove = { by -> onMove(game, by) },
                        modifier = Modifier
                            .offset { position }
                            .size(with(density) { cellWidth.toDp() }, with(density) { cellHeight.toDp() })
                            .zIndex(if (isDragged) 1f else 0f)
                    )
                }
            }
        }
    }
}

/** Icon and name. Taps reach it through the grid; TalkBack gets open and move actions directly. */
@Composable
private fun GameTile(
    game: GameType,
    index: Int,
    count: Int,
    lifted: Boolean,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier
) {
    val info = GameCatalog.infoFor(game)
    val title = stringResource(info.titleRes)
    val moveEarlier = stringResource(R.string.home_move_earlier)
    val moveLater = stringResource(R.string.home_move_later)
    Card(
        modifier = modifier
            .scale(if (lifted) LIFTED_SCALE else 1f)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = title) { onOpen(); true }
                customActions = buildList {
                    if (index > 0) add(CustomAccessibilityAction(moveEarlier) { onMove(-1); true })
                    if (index < count - 1) add(CustomAccessibilityAction(moveLater) { onMove(1); true })
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 12.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun Offset.round(): IntOffset = IntOffset(x.roundToInt(), y.roundToInt())

private val GAP = 16.dp
/** Height / width of a card: a little wider than tall. */
private const val CELL_ASPECT = 0.85f
private const val LIFTED_SCALE = 1.05f
