package top.yukonga.miuix.kmp.extra

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.ArrowUpDownIntegrated
import top.yukonga.miuix.kmp.icon.icons.basic.Check
import top.yukonga.miuix.kmp.interfaces.HoldDownInteraction
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A spinner component with Miuix style. (Popup Mode)
 *
 * @param items The list of [SpinnerEntry] to be shown in the [SuperSpinner].
 * @param selectedIndex The index of the selected item in the [SuperSpinner].
 * @param title The title of the [SuperSpinner].
 * @param titleColor The color of the title of the [SuperSpinner].
 * @param summary The summary of the [SuperSpinner].
 * @param summaryColor The color of the summary of the [SuperSpinner].
 * @param leftAction The action to be shown at the left side of the [SuperSpinner].
 * @param mode The mode of the [SuperSpinner].
 * @param modifier The [Modifier] to be applied to the [SuperSpinner].
 * @param insideMargin The [PaddingValues] to be applied inside the [SuperSpinner].
 * @param maxHeight The maximum height of the [ListPopup].
 * @param enabled Whether the [SuperSpinner] is enabled.
 * @param showValue Whether to show the value of the [SuperSpinner].
 * @param onClick The callback when the [SuperSpinner] is clicked.
 * @param onSelectedIndexChange The callback to be invoked when the selected index of the [SuperSpinner] is changed.
 */
@Composable
fun SuperSpinner(
    items: List<SpinnerEntry>,
    selectedIndex: Int,
    title: String,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    leftAction: @Composable (() -> Unit)? = null,
    mode: SpinnerMode = SpinnerMode.Normal,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    onClick: (() -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val showPopup = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val actionColor = if (enabled) MiuixTheme.colorScheme.onSurfaceVariantActions else MiuixTheme.colorScheme.disabledOnSecondaryVariant
    var alignLeft by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isDropdownExpanded.value) {
        showPopup.value = isDropdownExpanded.value
    }

    DisposableEffect(Unit) {
        onDispose {
            isDropdownExpanded.value = false
        }
    }

    BasicComponent(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (enabled) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Move) {
                            val eventChange = event.changes.first()
                            if (eventChange.pressed) {
                                alignLeft = eventChange.position.x < (size.width / 2)
                            }
                        }
                    }
                }
            },
        interactionSource = interactionSource,
        insideMargin = insideMargin,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        leftAction = {
            ListPopup(
                show = showPopup,
                alignment = if ((mode == SpinnerMode.AlwaysOnRight || !alignLeft))
                    PopupPositionProvider.Align.Right
                else
                    PopupPositionProvider.Align.Left,
                onDismissRequest = {
                    showPopup.value = false
                    isDropdownExpanded.value = false
                },
                maxHeight = maxHeight
            ) {
                ListPopupColumn {
                    items.forEachIndexed { index, spinnerEntry ->
                        SpinnerItemImpl(
                            entry = spinnerEntry,
                            entryCount = items.size,
                            isSelected = selectedIndex == index,
                            index = index,
                            dialogMode = false
                        ) { selectedIdx ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelectedIndexChange?.invoke(selectedIdx)
                            showPopup.value = false
                            isDropdownExpanded.value = false
                        }
                    }
                }
            }
            leftAction?.invoke()
        },
        rightActions = {
            if (showValue) {
                Text(
                    modifier = Modifier.widthIn(max = 130.dp),
                    text = items[selectedIndex].title ?: "",
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = actionColor,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            Image(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(10.dp, 16.dp)
                    .align(Alignment.CenterVertically),
                imageVector = MiuixIcons.Basic.ArrowUpDownIntegrated,
                colorFilter = ColorFilter.tint(actionColor),
                contentDescription = null
            )
        },
        onClick = {
            if (enabled) {
                onClick?.invoke()
                isDropdownExpanded.value = !isDropdownExpanded.value
                if (isDropdownExpanded.value) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            }
        },
        holdDownState = isDropdownExpanded.value,
        enabled = enabled
    )
}

/**
 * A [SuperSpinner] component with Miuix style, show Spinner as dialog. (Dialog Mode)
 *
 * @param items the list of [SpinnerEntry] to be shown in the [SuperSpinner].
 * @param selectedIndex the index of the selected item in the [SuperSpinner].
 * @param title the title of the [SuperSpinner].
 * @param titleColor the color of the title of the [SuperSpinner].
 * @param summary the summary of the [SuperSpinner].
 * @param summaryColor the color of the summary of the [SuperSpinner].
 * @param leftAction the action to be shown at the left side of the [SuperSpinner].
 * @param dialogButtonString the string of the button in the dialog.
 * @param popupModifier the [Modifier] to be applied to the popup of the [SuperSpinner].
 * @param modifier the [Modifier] to be applied to the [SuperSpinner].
 * @param insideMargin the [PaddingValues] to be applied inside the [SuperSpinner].
 * @param enabled whether the [SuperSpinner] is enabled.
 * @param showValue whether to show the value of the [SuperSpinner].
 * @param onClick the callback when the [SuperSpinner] is clicked.
 * @param onSelectedIndexChange the callback to be invoked when the selected index of the [SuperSpinner] is changed.
 */
@Composable
fun SuperSpinner(
    items: List<SpinnerEntry>,
    selectedIndex: Int,
    title: String,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    leftAction: @Composable (() -> Unit)? = null,
    dialogButtonString: String,
    popupModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    enabled: Boolean = true,
    showValue: Boolean = true,
    onClick: (() -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val isHoldDown = remember { mutableStateOf<HoldDownInteraction.HoldDown?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val actionColor = if (enabled) MiuixTheme.colorScheme.onSurfaceVariantActions else MiuixTheme.colorScheme.disabledOnSecondaryVariant
    var alignLeft by rememberSaveable { mutableStateOf(true) }
    var dropdownOffsetXPx by remember { mutableIntStateOf(0) }
    var dropdownOffsetYPx by remember { mutableIntStateOf(0) }
    var componentHeightPx by remember { mutableIntStateOf(0) }
    var componentWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(isDropdownExpanded.value) {
        if (!isDropdownExpanded.value) {
            isHoldDown.value?.let { oldValue ->
                coroutineScope.launch {
                    interactionSource.emit(HoldDownInteraction.Release(oldValue))
                }
                isHoldDown.value = null
            }
        }
    }

    BasicComponent(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (enabled) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Move) {
                            val eventChange = event.changes.first()
                            alignLeft = eventChange.position.x < (size.width / 2)
                        }
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                if (isDropdownExpanded.value) {
                    val positionInWindow = coordinates.positionInWindow()
                    dropdownOffsetXPx = positionInWindow.x.toInt()
                    dropdownOffsetYPx = positionInWindow.y.toInt()
                    componentHeightPx = coordinates.size.height
                    componentWidthPx = coordinates.size.width
                }
            },
        interactionSource = interactionSource,
        insideMargin = insideMargin,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        leftAction = leftAction,
        rightActions = {
            if (showValue) {
                Text(
                    modifier = Modifier.widthIn(max = 130.dp),
                    text = items[selectedIndex].title ?: "",
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = actionColor,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            Image(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(10.dp, 16.dp)
                    .align(Alignment.CenterVertically),
                imageVector = MiuixIcons.Basic.ArrowUpDownIntegrated,
                colorFilter = ColorFilter.tint(actionColor),
                contentDescription = null
            )
        },
        onClick = {
            if (enabled) {
                onClick?.invoke()
                isDropdownExpanded.value = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch {
                    interactionSource.emit(HoldDownInteraction.HoldDown().also {
                        isHoldDown.value = it
                    })
                }
            }
        },
        enabled = enabled
    )

    SuperDialog(
        modifier = popupModifier,
        title = title,
        show = isDropdownExpanded,
        onDismissRequest = {
            isDropdownExpanded.value = false
        },
        insideMargin = DpSize(0.dp, 24.dp)
    ) {
        Layout(
            content = {
                LazyColumn {
                    items(items.size) { index ->
                        SpinnerItemImpl(
                            entry = items[index],
                            entryCount = items.size,
                            isSelected = selectedIndex == index,
                            index = index,
                            dialogMode = true
                        ) { selectedIdx ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSelectedIndexChange?.let { it1 -> it1(selectedIdx) }
                            isDropdownExpanded.value = false
                        }
                    }
                }
                TextButton(
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 24.dp).fillMaxWidth(),
                    text = dialogButtonString,
                    minHeight = 50.dp,
                    onClick = {
                        isDropdownExpanded.value = false
                    }
                )
            }
        ) { measurables, constraints ->
            if (measurables.size != 2) {
                layout(0, 0) { }
            }
            val button = measurables[1].measure(constraints)
            val lazyList = measurables[0].measure(
                constraints.copy(
                    maxHeight = constraints.maxHeight - button.height
                )
            )
            layout(constraints.maxWidth, lazyList.height + button.height) {
                lazyList.place(0, 0)
                button.place(0, lazyList.height)
            }
        }
    }
}

/**
 * The implementation of the spinner.
 *
 * @param entry the [SpinnerEntry] to be shown in the spinner.
 * @param entryCount the count of the entries in the spinner.
 * @param isSelected whether the entry is selected.
 * @param index the index of the entry.
 * @param dialogMode whether the spinner is in dialog mode.
 * @param onSelectedIndexChange the callback to be invoked when the selected index of the spinner is changed.
 */
@Composable
fun SpinnerItemImpl(
    entry: SpinnerEntry,
    entryCount: Int,
    isSelected: Boolean,
    index: Int,
    dialogMode: Boolean = false,
    onSelectedIndexChange: (Int) -> Unit,
) {
    val additionalTopPadding = if (!dialogMode && index == 0) 20f.dp else 12f.dp
    val additionalBottomPadding = if (!dialogMode && index == entryCount - 1) 20f.dp else 12f.dp
    val titleColor: Color
    val summaryColor: Color
    val selectColor: Color
    val backgroundColor: Color
    if (isSelected) {
        titleColor = MiuixTheme.colorScheme.onTertiaryContainer
        summaryColor = MiuixTheme.colorScheme.onTertiaryContainer
        selectColor = MiuixTheme.colorScheme.onTertiaryContainer
        backgroundColor = MiuixTheme.colorScheme.tertiaryContainer
    } else {
        titleColor = MiuixTheme.colorScheme.onSurface
        summaryColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
        selectColor = Color.Transparent
        backgroundColor = MiuixTheme.colorScheme.surface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .clickable {
                onSelectedIndexChange(index)
            }
            .background(backgroundColor)
            .then(
                if (dialogMode) Modifier.heightIn(min = 56.dp).widthIn(min = 200.dp).fillMaxWidth().padding(horizontal = 28.dp)
                else Modifier.padding(horizontal = 20.dp)
            )
            .padding(top = additionalTopPadding, bottom = additionalBottomPadding)
    ) {
        Row(
            modifier = if (dialogMode) Modifier else Modifier.widthIn(max = 216.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            entry.icon?.let {
                it(
                    Modifier.sizeIn(minWidth = 26.dp, minHeight = 26.dp).padding(end = 12.dp)
                )
            }
            Column {
                entry.title?.let {
                    Text(
                        text = it,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )
                }
                entry.summary?.let {
                    Text(
                        text = it,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = summaryColor
                    )
                }
            }
        }
        Image(
            modifier = Modifier.padding(start = 12.dp).size(20.dp),
            imageVector = MiuixIcons.Basic.Check,
            colorFilter = BlendModeColorFilter(selectColor, BlendMode.SrcIn),
            contentDescription = null,
        )
    }
}

/**
 * The spinner entry.
 */
data class SpinnerEntry(
    val icon: @Composable ((Modifier) -> Unit)? = null,
    val title: String? = null,
    val summary: String? = null
)

/**
 * The spinner show mode.
 */
enum class SpinnerMode {
    Normal,
    AlwaysOnRight
}