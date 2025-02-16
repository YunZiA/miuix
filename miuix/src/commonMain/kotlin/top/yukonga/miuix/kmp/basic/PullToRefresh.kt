package top.yukonga.miuix.kmp.basic

import ProgressIndicator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefreshDefaults.Indicator
import top.yukonga.miuix.kmp.basic.PullToRefreshDefaults.IndicatorBox
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.js.JsName
import kotlin.math.abs
import kotlin.math.pow

/**
 * [PullToRefreshBox] is a container that expects a scrollable layout as content and adds gesture
 * support for manually refreshing when the user swipes downward at the beginning of the content. By
 * default, it uses [PullToRefreshDefaults.Indicator] as the refresh indicator.
 *
 * @param isRefreshing whether a refresh is occurring
 * @param onRefresh callback invoked when the user gesture crosses the threshold, thereby requesting
 *   a refresh.
 * @param modifier the [Modifier] to be applied to this container
 * @param state the state that keeps track of distance pulled
 * @param contentAlignment The default alignment inside the Box.
 * @param indicator the indicator that will be drawn on top of the content when the user begins a
 *   pull or a refresh is occurring
 * @param content the content of the pull refresh container, typically a scrollable layout such as
 *   [LazyColumn] or a layout using [Modifier.verticalScroll]
 */
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state
        )
    },
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.pullToRefresh(state = state, isRefreshing = isRefreshing, onRefresh = onRefresh),
        contentAlignment = contentAlignment
    ) {
        content()
        indicator()
    }
}

/**
 * A Modifier that adds nested scroll to a container to support a pull-to-refresh gesture. When the
 * user pulls a distance greater than [threshold] and releases the gesture, [onRefresh] is invoked.
 * [PullToRefreshBox] applies this automatically.
 *
 * @param isRefreshing whether a refresh is occurring or not, if there is no gesture in progress
 *   when isRefreshing is false the `state.distanceFraction` will animate to 0f, otherwise it will
 *   animate to 1f
 * @param state state that keeps track of the distance pulled
 * @param enabled whether nested scroll events should be consumed by this modifier
 * @param threshold how much distance can be scrolled down before [onRefresh] is invoked
 * @param onRefresh callback that is invoked when the distance pulled is greater than [threshold]
 */

fun Modifier.pullToRefresh(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    enabled: Boolean = true,
    threshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    onRefresh: () -> Unit,
): Modifier =
    this then
            PullToRefreshElement(
                state = state,
                isRefreshing = isRefreshing,
                enabled = enabled,
                onRefresh = onRefresh,
                threshold = threshold
            )


internal data class PullToRefreshElement(
    val isRefreshing: Boolean,
    val onRefresh: () -> Unit,
    val enabled: Boolean,
    val state: PullToRefreshState,
    val threshold: Dp,
) : ModifierNodeElement<PullToRefreshModifierNode>() {
    override fun create() =
        PullToRefreshModifierNode(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            enabled = enabled,
            state = state,
            threshold = threshold
        )

    override fun update(node: PullToRefreshModifierNode) {
        node.onRefresh = onRefresh
        node.enabled = enabled
        node.state = state
        node.threshold = threshold
        if (node.isRefreshing != isRefreshing) {
            node.isRefreshing = isRefreshing
            node.update()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "PullToRefreshModifierNode"
        properties["isRefreshing"] = isRefreshing
        properties["onRefresh"] = onRefresh
        properties["enabled"] = enabled
        properties["state"] = state
        properties["threshold"] = threshold
    }
}


internal class PullToRefreshModifierNode(
    var isRefreshing: Boolean,
    var onRefresh: () -> Unit,
    var enabled: Boolean,
    var state: PullToRefreshState,
    var threshold: Dp,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, NestedScrollConnection {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private var nestedScrollNode: DelegatableNode =
        nestedScrollModifierNode(
            connection = this,
            dispatcher = null,
        )

    private var verticalOffset by mutableFloatStateOf(0f)
    private var distancePulled by mutableFloatStateOf(0f)

    private val adjustedDistancePulled: Float
        get() = distancePulled * DragMultiplier

    private val thresholdPx
        get() = with(currentValueOf(LocalDensity)) { threshold.roundToPx() }

    private val progress
        get() = adjustedDistancePulled / thresholdPx

    override fun onAttach() {
        delegate(nestedScrollNode)
        coroutineScope.launch { state.snapTo(if (isRefreshing) 1f else 0f) }
        verticalOffset = if (isRefreshing) thresholdPx.toFloat() else 0f
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        when {
            state.isAnimating -> Offset.Zero
            !enabled -> Offset.Zero
            // Swiping up
            source == NestedScrollSource.UserInput && available.y < 0 -> {
                consumeAvailableOffset(available)
            }

            else -> Offset.Zero
        }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset =
        when {
            state.isAnimating -> Offset.Zero
            !enabled -> Offset.Zero
            // Swiping down
            source == NestedScrollSource.UserInput -> {
                val newOffset = consumeAvailableOffset(available)
                coroutineScope.launch { state.snapTo(verticalOffset / thresholdPx) }

                newOffset
            }

            else -> Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity(0f, onRelease(available.y))
    }

    fun update() {
        coroutineScope.launch {
            if (!isRefreshing) {
                animateToHidden()
            } else {
                animateToThreshold()
            }
        }
    }

    /** Helper method for nested scroll connection */
    private fun consumeAvailableOffset(available: Offset): Offset {
        val y =
            if (isRefreshing) 0f
            else {
                val newOffset = (distancePulled + available.y).coerceAtLeast(0f)
                val dragConsumed = newOffset - distancePulled
                distancePulled = newOffset
                verticalOffset = calculateVerticalOffset()
                dragConsumed
            }
        return Offset(0f, y)
    }

    /** Helper method for nested scroll connection. Calls onRefresh callback when triggered */
    private suspend fun onRelease(velocity: Float): Float {
        if (isRefreshing) return 0f // Already refreshing, do nothing
        // Trigger refresh
        if (adjustedDistancePulled > thresholdPx) {
            onRefresh()
        }

        val consumed =
            when {
                // We are flinging without having dragged the pull refresh (for example a fling
                // inside a list) - don't consume
                distancePulled == 0f -> 0f
                // If the velocity is negative, the fling is upwards, and we don't want to prevent
                // the list from scrolling
                velocity < 0f -> 0f
                // We are showing the indicator, and the fling is downwards - consume everything
                else -> velocity
            }

        animateToHidden()

        distancePulled = 0f

        return consumed
    }

    private fun calculateVerticalOffset(): Float =
        when {
            // If drag hasn't gone past the threshold, the position is the adjustedDistancePulled.
            adjustedDistancePulled <= thresholdPx -> adjustedDistancePulled
            else -> {
                // How far beyond the threshold pull has gone, as a percentage of the threshold.
                val overshootPercent = abs(progress) - 1.0f
                // Limit the overshoot to 200%. Linear between 0 and 200.
                val linearTension = overshootPercent.coerceIn(0f, 2f)
                // Non-linear tension. Increases with linearTension, but at a decreasing rate.
                val tensionPercent = linearTension - linearTension.pow(2) / 4
                // The additional offset beyond the threshold.
                val extraOffset = thresholdPx * tensionPercent
                thresholdPx + extraOffset
            }
        }

    private suspend fun animateToThreshold() {
        state.animateToThreshold()
        distancePulled = thresholdPx.toFloat()
        verticalOffset = thresholdPx.toFloat()
    }

    private suspend fun animateToHidden() {
        state.animateToHidden()
        distancePulled = 0f
        verticalOffset = 0f
    }
}

/** Contains the default values for [PullToRefreshBox] */

object PullToRefreshDefaults {
    /** The default shape for [Indicator] */
    val shape: Shape = CircleShape

    /** The default container color for [Indicator] */
    @Deprecated(
        "Use loadingIndicatorContainerColor instead",
        ReplaceWith("loadingIndicatorContainerColor")
    )
    val containerColor: Color
        @Composable get() = MiuixTheme.colorScheme.surfaceVariant

    /** The default indicator color for [Indicator] */
    @Deprecated("Use loadingIndicatorColor instead", ReplaceWith("loadingIndicatorColor"))
    val indicatorColor: Color
        @Composable get() = MiuixTheme.colorScheme.primary

    /** The default refresh threshold for [rememberPullToRefreshState] */
    val PositionalThreshold = 100.dp

    /** The default elevation for an [IndicatorBox] that is applied to an [Indicator] */
    val Elevation = 3.dp

    /**
     * A Wrapper that handles the size, offset, clipping, shadow, and background drawing for a
     * pull-to-refresh indicator, useful when implementing custom indicators.
     * [PullToRefreshDefaults.Indicator] uses this as the container.
     *
     * @param state the state of this modifier, will use `state.distanceFraction` and [threshold] to
     *   calculate the offset
     * @param isRefreshing whether a refresh is occurring
     * @param modifier the modifier applied to this layout
     * @param threshold how much the indicator can be pulled down before a refresh is triggered on
     *   release
     * @param shape the [Shape] of this indicator
     * @param containerColor the container color of this indicator
     * @param elevation the elevation for the indicator
     * @param content content for this [IndicatorBox]
     */
    @Composable
    fun IndicatorBox(
        state: PullToRefreshState,
        isRefreshing: Boolean,
        modifier: Modifier = Modifier,
        threshold: Dp = PositionalThreshold,
        shape: Shape = PullToRefreshDefaults.shape,
        containerColor: Color = Color.Unspecified,
        elevation: Dp = Elevation,
        content: @Composable BoxScope.() -> Unit
    ) {
        Box(
            modifier =
                modifier
                    .size(SpinnerContainerSize)
                    .drawWithContent {
                        clipRect(
                            top = 0f,
                            left = -Float.MAX_VALUE,
                            right = Float.MAX_VALUE,
                            bottom = Float.MAX_VALUE
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeWithLayer(
                                0,
                                0,
                                layerBlock = {
                                    val showElevation = state.distanceFraction > 0f || isRefreshing
                                    translationY =
                                        state.distanceFraction * threshold.roundToPx() - size.height
                                    shadowElevation = if (showElevation) elevation.toPx() else 0f
                                    this.shape = shape
                                    clip = true
                                }
                            )
                        }
                    }
                    .background(color = containerColor, shape = shape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }

    /**
     * The default indicator for [PullToRefreshBox].
     *
     * @param state the state of this modifier, will use `state.distanceFraction` and [threshold] to
     *   calculate the offset
     * @param isRefreshing whether a refresh is occurring
     * @param modifier the modifier applied to this layout
     * @param containerColor the container color of this indicator
     * @param color the color of this indicator
     * @param threshold how much the indicator can be pulled down before a refresh is triggered on
     *   release
     */

    @Suppress("DEPRECATION")
    @Composable
    fun Indicator(
        state: PullToRefreshState,
        isRefreshing: Boolean,
        modifier: Modifier = Modifier,
        containerColor: Color = this.containerColor,
        color: Color = this.indicatorColor,
        threshold: Dp = PositionalThreshold,
    ) {
        IndicatorBox(
            modifier = modifier,
            state = state,
            isRefreshing = isRefreshing,
            containerColor = containerColor,
            threshold = threshold,
        ) {
            Crossfade(
                targetState = isRefreshing,
                animationSpec = tween(durationMillis = 300)
            ) { refreshing ->
                if (refreshing) {
                    ProgressIndicator(
                        color = color,
                        modifier = Modifier.size(SpinnerSize),
                    )
                } else {
                    ProgressIndicator(
                        color = color,
                        rotation = state.distanceFraction * 180,
                    )
                }
            }
        }
    }
}

/**
 * The state of a [PullToRefreshBox] which tracks the distance that the container and indicator have
 * been pulled.
 *
 * Each instance of [PullToRefreshBox] should have its own [PullToRefreshState].
 *
 */
@Stable

interface PullToRefreshState {

    /**
     * Distance percentage towards the refresh threshold. 0.0 indicates no distance, 1.0 indicates
     * being at the threshold offset, > 1.0 indicates overshoot beyond the provided threshold.
     */
    val distanceFraction: Float

    /**
     * whether the state is currently animating the indicator to the threshold offset, or back to
     * the hidden offset
     */
    val isAnimating: Boolean

    /**
     * Animate the distance towards the anchor or threshold position, where the indicator will be
     * shown when refreshing.
     */
    suspend fun animateToThreshold()

    /** Animate the distance towards the position where the indicator will be hidden when idle */
    suspend fun animateToHidden()

    /** Snap the indicator to the desired threshold fraction */
    suspend fun snapTo(targetValue: Float)
}

/** Create and remember the default [PullToRefreshState]. */
@Composable

fun rememberPullToRefreshState(): PullToRefreshState {
    return rememberSaveable(saver = PullToRefreshStateImpl.Saver) { PullToRefreshStateImpl() }
}

/**
 * Creates a [PullToRefreshState].
 *
 * Note that in most cases, you are advised to use [rememberPullToRefreshState] when in composition.
 */
@JsName("funPullToRefreshState")

fun PullToRefreshState(): PullToRefreshState = PullToRefreshStateImpl()

internal class PullToRefreshStateImpl
private constructor(private val anim: Animatable<Float, AnimationVector1D>) : PullToRefreshState {
    constructor() : this(Animatable(0f, Float.VectorConverter))

    override val distanceFraction
        get() = anim.value

    /** Whether the state is currently animating */
    override val isAnimating: Boolean
        get() = anim.isRunning

    override suspend fun animateToThreshold() {
        anim.animateTo(1f)
    }

    override suspend fun animateToHidden() {
        anim.animateTo(0f)
    }

    override suspend fun snapTo(targetValue: Float) {
        anim.snapTo(targetValue)
    }

    companion object {
        val Saver =
            Saver<PullToRefreshStateImpl, Float>(
                save = { it.anim.value },
                restore = { PullToRefreshStateImpl(Animatable(it, Float.VectorConverter)) }
            )
    }
}

/** The default stroke width for [Indicator] */
internal val SpinnerSize = 16.dp // (ArcRadius + PullRefreshIndicatorDefaults.StrokeWidth).times(2)
internal val SpinnerContainerSize = 40.dp

/**
 * The distance pulled is multiplied by this value to give us the adjusted distance pulled, which is
 * used in calculating the indicator position (when the adjusted distance pulled is less than the
 * refresh threshold, it is the indicator position, otherwise the indicator position is derived from
 * the progress).
 */
private const val DragMultiplier = 0.5f