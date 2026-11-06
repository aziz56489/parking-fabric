package com.parking.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.parking.app.Item
import com.parking.app.ItemKind
import com.parking.app.SpotStatus
import com.parking.app.SpotType
import com.parking.app.contains
import kotlin.math.min

// Same colours as the web editor (frontend/src/app/layout-editor/plan-items.ts).
private fun colorOf(kind: ItemKind, type: SpotType?): Color = when (kind) {
    ItemKind.SPOT -> when (type) {
        SpotType.EV -> Color(0xFF16A34A)
        SpotType.HANDICAPPED -> Color(0xFF9333EA)
        SpotType.PERSONNEL -> Color(0xFFEA580C)
        else -> Color(0xFF2563EB)
    }
    ItemKind.ROAD -> Color(0xFF52525B)
    ItemKind.ENTRANCE -> Color(0xFFCA8A04)
    ItemKind.EXIT -> Color(0xFFDC2626)
    ItemKind.PILLAR, ItemKind.WALL -> Color(0xFF78350F)
    ItemKind.GREEN_SPACE -> Color(0xFF65A30D)
}

private val TakenColor = Color(0xFFA1A1AA)
private val HighlightColor = Color(0xFFFACC15)

private fun labelOf(item: Item): String = when (item.kind) {
    ItemKind.SPOT -> item.number ?: ""
    ItemKind.ENTRANCE -> "IN"
    ItemKind.EXIT -> "OUT"
    else -> ""
}

/** Pixels per metre and top-left corner that fit the whole plan in the canvas, centred. */
private fun fit(canvas: Size, planWidth: Double, planHeight: Double): Pair<Float, Offset> {
    val scale = 0.96f * min(canvas.width / planWidth, canvas.height / planHeight).toFloat()
    val origin = Offset(
        (canvas.width - planWidth.toFloat() * scale) / 2,
        (canvas.height - planHeight.toFloat() * scale) / 2,
    )
    return scale to origin
}

/**
 * Read-only 2D plan of one level: pinch to zoom, drag to move, tap a spot to select it.
 * Spots that are not available are grey; [highlighted] (the selected spot or the user's spot) is yellow.
 * Wrap it in key(areaId, floor) { } so zoom resets when the level changes.
 */
@Composable
fun ParkingPlan(
    items: List<Item>,
    planWidth: Double,
    planHeight: Double,
    statuses: Map<String, SpotStatus>,
    highlighted: String?,
    onSpotTap: ((Item) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val drawOrder = remember(items) { items.sortedBy { it.kind.layer } }
    val currentItems by rememberUpdatedState(drawOrder)
    val currentOnTap by rememberUpdatedState(onSpotTap)
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clipToBounds()
            .background(Color(0xFFE4E4E7))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(1f, 8f)
                    // Keep the point under the fingers in place, and the plan inside the view.
                    val newPan = centroid - (centroid - pan) * (newZoom / zoom) + panChange
                    zoom = newZoom
                    pan = Offset(
                        newPan.x.coerceIn(size.width * (1 - newZoom), 0f),
                        newPan.y.coerceIn(size.height * (1 - newZoom), 0f),
                    )
                }
            },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                }
                .pointerInput(planWidth, planHeight) {
                    // Inside graphicsLayer, so the tap is already in unzoomed canvas pixels.
                    detectTapGestures { tap ->
                        val (scale, origin) = fit(Size(size.width.toFloat(), size.height.toFloat()), planWidth, planHeight)
                        val x = (tap.x - origin.x) / scale.toDouble()
                        val y = (tap.y - origin.y) / scale.toDouble()
                        currentItems.lastOrNull { it.kind == ItemKind.SPOT && it.contains(x, y) }
                            ?.let { currentOnTap?.invoke(it) }
                    }
                },
        ) {
            val (scale, origin) = fit(size, planWidth, planHeight)
            val planSize = Size(planWidth.toFloat() * scale, planHeight.toFloat() * scale)

            drawRect(Color.White, origin, planSize)
            // Thin line every 5 m, like the editor's thick grid lines.
            for (metre in 0..planWidth.toInt() step 5) {
                val x = origin.x + metre * scale
                drawLine(Color(0xFFE4E4E7), Offset(x, origin.y), Offset(x, origin.y + planSize.height), 1f)
            }
            for (metre in 0..planHeight.toInt() step 5) {
                val y = origin.y + metre * scale
                drawLine(Color(0xFFE4E4E7), Offset(origin.x, y), Offset(origin.x + planSize.width, y), 1f)
            }

            for (item in drawOrder) {
                val topLeft = origin + Offset(item.x.toFloat() * scale, item.y.toFloat() * scale)
                val itemSize = Size(item.width.toFloat() * scale, item.height.toFloat() * scale)
                val isSpot = item.kind == ItemKind.SPOT
                val isHighlighted = isSpot && item.number == highlighted
                val fill = when {
                    !isSpot -> colorOf(item.kind, item.type)
                    isHighlighted -> HighlightColor
                    statuses[item.number] == SpotStatus.AVAILABLE -> colorOf(item.kind, item.type)
                    else -> TakenColor
                }

                // Like Fabric with its default top-left origin: the shape turns around (x, y).
                rotate(item.angle.toFloat(), pivot = topLeft) {
                    drawRect(fill, topLeft, itemSize)
                    if (isSpot) {
                        val border = if (isHighlighted) 0.25f else 0.1f
                        drawRect(
                            if (isHighlighted) Color.Black else Color.White,
                            topLeft,
                            itemSize,
                            style = Stroke(width = border * scale),
                        )
                    }
                    val label = labelOf(item)
                    if (label.isNotEmpty()) {
                        val text = textMeasurer.measure(
                            label,
                            TextStyle(
                                color = if (isHighlighted) Color.Black else Color.White,
                                fontSize = (0.7f * scale).toSp(), // 0.7 m, like the editor
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        drawText(
                            text,
                            topLeft = topLeft + Offset(
                                (itemSize.width - text.size.width) / 2,
                                (itemSize.height - text.size.height) / 2,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanLegend(highlightLabel: String, modifier: Modifier = Modifier) {
    val entries = SpotType.entries.map { it.label to colorOf(ItemKind.SPOT, it) } +
        listOf("Taken" to TakenColor, highlightLabel to HighlightColor)
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((label, color) in entries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color))
                Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
