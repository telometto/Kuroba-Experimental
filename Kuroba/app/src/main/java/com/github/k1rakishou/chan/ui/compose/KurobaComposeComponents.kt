package com.github.k1rakishou.chan.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.core_themes.ChanTheme
import com.github.k1rakishou.core_themes.ThemeEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.math.roundToInt

private val DefaultFillMaxSizeModifier: Modifier = Modifier.fillMaxSize()

@Composable
fun KurobaComposeProgressIndicator(modifier: Modifier = DefaultFillMaxSizeModifier, overrideColor: Color? = null) {
  Box(modifier = modifier) {
    val color = if (overrideColor == null) {
      val chanTheme = LocalChanTheme.current
      remember(key1 = chanTheme.accentColor) { Color(chanTheme.accentColor) }
    } else {
      overrideColor
    }

    CircularProgressIndicator(
      color = color,
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}

@Composable
fun KurobaComposeErrorMessage(error: Throwable, modifier: Modifier = DefaultFillMaxSizeModifier) {
  KurobaComposeErrorMessage(error.errorMessageOrClassName(), modifier)
}

@Composable
fun KurobaComposeErrorMessage(errorMessage: String, modifier: Modifier = DefaultFillMaxSizeModifier) {
  Box(modifier = Modifier
    .padding(8.dp)
    .then(modifier)
  ) {
    KurobaComposeText(errorMessage, modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun KurobaComposeText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color? = null,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontWeight: FontWeight? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  softWrap: Boolean = true,
  enabled: Boolean = true,
  textAlign: TextAlign? = null,
  inlineContent: Map<String, InlineTextContent> = mapOf()
) {
  KurobaComposeText(
    text = AnnotatedString(text),
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    maxLines = maxLines,
    overflow = overflow,
    softWrap = softWrap,
    enabled = enabled,
    textAlign = textAlign,
    inlineContent = inlineContent
  )
}

@Composable
fun KurobaComposeText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  color: Color? = null,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontWeight: FontWeight? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  softWrap: Boolean = true,
  enabled: Boolean = true,
  textAlign: TextAlign? = null,
  inlineContent: Map<String, InlineTextContent> = mapOf()
) {
  val textColorPrimary = if (color == null) {
    val chanTheme = LocalChanTheme.current

    remember(key1 = chanTheme.textColorPrimary) {
      Color(chanTheme.textColorPrimary)
    }
  } else {
    color
  }

  val actualTextColorPrimary = if (enabled) {
    textColorPrimary
  } else {
    textColorPrimary.copy(alpha = ContentAlpha.disabled)
  }

  Text(
    color = actualTextColorPrimary,
    text = text,
    fontSize = fontSize,
    maxLines = maxLines,
    overflow = overflow,
    softWrap = softWrap,
    textAlign = textAlign,
    fontWeight = fontWeight,
    inlineContent = inlineContent,
    modifier = modifier,
  )
}

@Composable
fun KurobaComposeClickableText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  color: Color? = null,
  fontSize: TextUnit = TextUnit.Unspecified,
  fontWeight: FontWeight? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  softWrap: Boolean = true,
  textAlign: TextAlign? = null,
  inlineContent: Map<String, InlineTextContent> = mapOf(),
  onTextClicked: (TextLayoutResult, Int) -> Boolean
) {
  val textColorPrimary = if (color == null) {
    val chanTheme = LocalChanTheme.current

    remember(key1 = chanTheme.textColorPrimary) {
      Color(chanTheme.textColorPrimary)
    }
  } else {
    color
  }

  val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

  val pressIndicatorModifier = Modifier.pointerInput(key1 = onTextClicked) {
    forEachGesture {
      awaitPointerEventScope {
        val downPointerInputChange = awaitFirstDown()

        val upOrCancelPointerInputChange = waitForUpOrCancellation()
          ?: return@awaitPointerEventScope

        val result = layoutResult.value
          ?: return@awaitPointerEventScope

        val offset = result.getOffsetForPosition(upOrCancelPointerInputChange.position)

        if (onTextClicked.invoke(result, offset)) {
          downPointerInputChange.consumeAllChanges()
          upOrCancelPointerInputChange.consumeAllChanges()
        }
      }
    }
  }

  Text(
    color = textColorPrimary,
    text = text,
    fontSize = fontSize,
    maxLines = maxLines,
    overflow = overflow,
    softWrap = softWrap,
    textAlign = textAlign,
    fontWeight = fontWeight,
    inlineContent = inlineContent,
    modifier = modifier.then(pressIndicatorModifier),
    onTextLayout = { textLayoutResult -> layoutResult.value = textLayoutResult }
  )
}

@Composable
fun KurobaComposeTextField(
  value: String,
  modifier: Modifier = Modifier,
  onValueChange: (String) -> Unit,
  maxLines: Int = Int.MAX_VALUE,
  singleLine: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions(),
  textStyle: TextStyle = LocalTextStyle.current,
  enabled: Boolean = true,
  label: @Composable (() -> Unit)? = null
) {
  val chanTheme = LocalChanTheme.current

  val textSelectionColors = remember(key1 = chanTheme.accentColorCompose) {
    TextSelectionColors(
      handleColor = chanTheme.accentColorCompose,
      backgroundColor = chanTheme.accentColorCompose.copy(alpha = 0.4f)
    )
  }

  CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
    TextField(
      enabled = enabled,
      value = value,
      label = label,
      onValueChange = onValueChange,
      maxLines = maxLines,
      singleLine = singleLine,
      modifier = modifier,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      colors = chanTheme.textFieldColors(),
      textStyle = textStyle
    )
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KurobaComposeCustomTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textColor: Color = Color.Unspecified,
  onBackgroundColor: Color = Color.Unspecified,
  drawBottomIndicator: Boolean = true,
  fontSize: TextUnit = TextUnit.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  singleLine: Boolean = false,
  labelText: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions(),
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
  val chanTheme = LocalChanTheme.current
  val textFieldColors = chanTheme.textFieldColors()

  val cursorBrush = remember(key1 = chanTheme) { SolidColor(chanTheme.accentColorCompose) }

  val actualTextColor = if (!textColor.isUnspecified) {
    textColor
  } else {
    if (ThemeEngine.isDarkColor(onBackgroundColor)) {
      Color.White
    } else {
      Color.Black
    }
  }

  val textStyle = remember(key1 = actualTextColor, key2 = fontSize) {
    TextStyle.Default.copy(color = actualTextColor, fontSize = fontSize)
  }

  val indicatorLineModifier = if (drawBottomIndicator) {
    val indicatorColorState = textFieldColors.indicatorColor(
      enabled = enabled,
      isError = false,
      interactionSource = interactionSource
    )

    Modifier.drawIndicatorLine(
      color = indicatorColorState.value,
      lineWidth = 2.dp,
      verticalOffset = 2.dp
    )
  } else {
    Modifier
  }

  val textSelectionColors = remember(key1 = chanTheme.accentColorCompose) {
    TextSelectionColors(
      handleColor = chanTheme.accentColorCompose,
      backgroundColor = chanTheme.accentColorCompose.copy(alpha = 0.4f)
    )
  }

  var localInput by remember { mutableStateOf(value) }

  Box {
    if (labelText != null) {
      val isFocused by interactionSource.collectIsFocusedAsState()

      AnimatedVisibility(
        visible = !enabled || (!isFocused && localInput.isEmpty()),
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        val alpha = if (enabled) {
          ContentAlpha.medium
        } else {
          ContentAlpha.disabled
        }

        val hintColor = remember(key1 = onBackgroundColor) {
          if (onBackgroundColor.isUnspecified) {
            Color.DarkGray.copy(alpha = alpha)
          } else {
            if (ThemeEngine.isDarkColor(onBackgroundColor)) {
              Color.LightGray.copy(alpha = alpha)
            } else {
              Color.DarkGray.copy(alpha = alpha)
            }
          }
        }

        Text(
          text = labelText,
          fontSize = 16.sp,
          color = hintColor
        )
      }
    }

    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
      BasicTextField(
        modifier = modifier
          .then(indicatorLineModifier)
          .padding(bottom = 4.dp),
        enabled = enabled,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = cursorBrush,
        value = value,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        onValueChange = { text ->
          localInput = text
          onValueChange(text)
        }
      )
    }
  }

}

private fun Modifier.drawIndicatorLine(
  color: Color,
  lineWidth: Dp = 1.dp,
  verticalOffset: Dp = Dp.Unspecified
): Modifier {
  return drawBehind {
    val strokeWidth = lineWidth.value * density
    val y = size.height - strokeWidth / 2

    val drawFunc = {
      drawLine(
        color,
        Offset(0f, y),
        Offset(size.width, y),
        strokeWidth
      )
    }

    if (verticalOffset.isSpecified) {
      translate(top = verticalOffset.toPx()) {
        drawFunc()
      }
    } else {
      drawFunc()
    }
  }
}

@Composable
fun KurobaComposeCheckbox(
  currentlyChecked: Boolean,
  onCheckChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  text: String? = null,
  enabled: Boolean = true
) {
  val chanTheme = LocalChanTheme.current
  var isChecked by remember(key1 = currentlyChecked) { mutableStateOf(currentlyChecked) }

  val color = remember(key1 = chanTheme) {
    if (chanTheme.isLightTheme) {
      Color(0x40000000)
    } else {
      Color(0x40ffffff)
    }
  }

  Row(
    modifier = Modifier
      .clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = true, color = color),
        onClick = {
          isChecked = isChecked.not()
          onCheckChanged(isChecked)
        }
      )
      .then(modifier)
  ) {
    Checkbox(
      modifier = Modifier.align(Alignment.CenterVertically),
      checked = isChecked,
      enabled = enabled,
      onCheckedChange = { checked ->
        isChecked = checked
        onCheckChanged(isChecked)
      },
      colors = chanTheme.checkBoxColors()
    )

    if (text != null) {
      Spacer(modifier = Modifier.width(8.dp))

      KurobaComposeText(
        modifier = Modifier.align(Alignment.CenterVertically),
        text = text,
        enabled = enabled
      )
    }
  }
}

@Composable
fun KurobaComposeTextButton(
  onClick: () -> Unit,
  text: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  KurobaComposeButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier,
    buttonContent = {
      Text(
        text = text,
        modifier = Modifier.fillMaxSize(),
        textAlign = TextAlign.Center
      )
    }
  )
}

@Composable
fun KurobaComposeButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  buttonContent: @Composable RowScope.() -> Unit
) {
  val chanTheme = LocalChanTheme.current

  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .wrapContentWidth()
      .height(36.dp)
      .then(modifier),
    content = buttonContent,
    colors = chanTheme.buttonColors()
  )
}

@Composable
fun KurobaComposeTextBarButton(
  onClick: () -> Unit,
  text: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val chanTheme = LocalChanTheme.current

  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .wrapContentSize()
      .then(modifier),
    content = {
      val textColor = if (enabled) {
        chanTheme.textColorPrimaryCompose
      } else {
        chanTheme.textColorPrimaryCompose.copy(alpha = ContentAlpha.disabled)
      }

      Text(
        text = text.uppercase(Locale.ENGLISH),
        color = textColor,
        modifier = Modifier
          .wrapContentSize()
          .align(Alignment.CenterVertically),
        textAlign = TextAlign.Center
      )
    },
    elevation = null,
    colors = chanTheme.barButtonColors()
  )
}

@Composable
fun KurobaComposeSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  steps: Int = 0
) {
  val chanTheme = LocalChanTheme.current

  Slider(
    value = value,
    modifier = modifier,
    steps = steps,
    onValueChange = onValueChange,
    colors = chanTheme.sliderColors()
  )
}

@Composable
fun KurobaComposeIcon(
  @DrawableRes drawableId: Int,
  themeEngine: ThemeEngine,
  modifier: Modifier = Modifier,
  colorBelowIcon: Color? = null
) {
  val chanTheme = LocalChanTheme.current
  val tintColor = remember(key1 = chanTheme) {
    if (colorBelowIcon == null) {
      Color(themeEngine.resolveDrawableTintColor())
    } else {
      Color(themeEngine.resolveDrawableTintColor(ThemeEngine.isDarkColor(colorBelowIcon.value)))
    }
  }

  Image(
    modifier = modifier,
    painter = painterResource(id = drawableId),
    colorFilter = ColorFilter.tint(tintColor),
    contentDescription = null
  )
}

private val defaultNoopClickCallback = { }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.kurobaClickable(
  bounded: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null
): Modifier {
  if (onLongClick == null && onClick == null) {
    error("At least one of the callbacks must be non-null")
  }

  return composed {
    val chanTheme = LocalChanTheme.current

    val color = remember(key1 = chanTheme) {
      if (chanTheme.isLightTheme) {
        Color(0x40000000)
      } else {
        Color(0x40ffffff)
      }
    }

    then(
      Modifier.combinedClickable(
        indication = rememberRipple(bounded = bounded, color = color),
        interactionSource = remember { MutableInteractionSource() },
        onLongClick = onLongClick,
        onClick = onClick ?: defaultNoopClickCallback
      )
    )
  }
}

@Composable
fun KurobaComposeCardView(
  modifier: Modifier = Modifier,
  backgroundColor: Color? = null,
  shape: Shape = RoundedCornerShape(2.dp),
  content: @Composable () -> Unit
) {
  val chanTheme = LocalChanTheme.current

  Card(
    modifier = modifier,
    shape = shape,
    backgroundColor = backgroundColor ?: chanTheme.backColorCompose
  ) {
    content()
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KurobaSearchInput(
  modifier: Modifier = Modifier,
  chanTheme: ChanTheme,
  themeEngine: ThemeEngine,
  onBackgroundColor: Color,
  searchQueryState: MutableState<String>,
  onSearchQueryChanged: (String) -> Unit,
  labelText: String = stringResource(id = R.string.search_hint)
) {
  var localQuery by remember { searchQueryState }

  Row(modifier = modifier) {
    Row(modifier = Modifier.wrapContentHeight()) {
      Box(modifier = Modifier
        .wrapContentHeight()
        .weight(1f)
        .align(Alignment.CenterVertically)
        .padding(horizontal = 4.dp)
      ) {
        val interactionSource = remember { MutableInteractionSource() }

        val textColor = remember(key1 = onBackgroundColor) {
          if (ThemeEngine.isDarkColor(onBackgroundColor)) {
            Color.White
          } else {
            Color.Black
          }
        }

        KurobaComposeCustomTextField(
          modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
          textColor = textColor,
          onBackgroundColor = onBackgroundColor,
          fontSize = 16.sp,
          singleLine = true,
          maxLines = 1,
          value = localQuery,
          labelText = labelText,
          onValueChange = { newValue ->
            localQuery = newValue
            onSearchQueryChanged(newValue)
          },
          interactionSource = interactionSource
        )
      }

      AnimatedVisibility(
        visible = localQuery.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        KurobaComposeIcon(
          modifier = Modifier
            .align(Alignment.CenterVertically)
            .kurobaClickable(
              bounded = false,
              onClick = {
                localQuery = ""
                onSearchQueryChanged("")
              }
            ),
          drawableId = R.drawable.ic_clear_white_24dp,
          themeEngine = themeEngine,
          colorBelowIcon = chanTheme.primaryColorCompose
        )
      }
    }
  }
}

@Composable
fun KurobaComposeSwitch(
  modifier: Modifier = Modifier,
  initiallyChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val chanTheme = LocalChanTheme.current

  Switch(
    checked = initiallyChecked,
    onCheckedChange = onCheckedChange,
    colors = chanTheme.switchColors(),
    modifier = modifier
  )
}

@Composable
fun KurobaComposeSnappingSlider(
  modifier: Modifier = Modifier,
  slideOffsetState: MutableState<Float>,
  onValueChange: (Float) -> Unit,
  backgroundColor: Color,
  slideSteps: Int? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
  val chanTheme = LocalChanTheme.current
  val density = LocalDensity.current
  val thumbRadiusNormal = with(density) { 12.dp.toPx() }
  val thumbRadiusPressed = with(density) { 16.dp.toPx() }
  val trackWidth = with(density) { 3.dp.toPx() }
  val slideOffset by slideOffsetState

  BoxWithConstraints(
    modifier = modifier
      .then(
        Modifier
          .widthIn(min = 144.dp)
          .height(32.dp)
      )
  ) {
    val trackColor = chanTheme.accentColorCompose
    val thumbColorNormal = remember(key1 = backgroundColor) {
      if (ThemeEngine.isDarkColor(trackColor)) {
        Color.LightGray
      } else {
        Color.DarkGray
      }
    }

    val thumbColorPressed = remember(key1 = thumbColorNormal) {
      if (ThemeEngine.isDarkColor(thumbColorNormal)) {
        ThemeEngine.manipulateColor(thumbColorNormal, 1.2f)
      } else {
        ThemeEngine.manipulateColor(thumbColorNormal, 0.8f)
      }
    }

    val maxWidthPx = constraints.maxWidth.toFloat()
    val rawOffsetState = remember { mutableStateOf(0f) }
    var rawOffsetInPx by rawOffsetState

    fun rawOffsetToUserValue(rawOffsetPx: Float, maxWidthPx: Float): Float {
      if (slideSteps != null) {
        val slideStepPx = (maxWidthPx / slideSteps.coerceAtLeast(1).toFloat()).coerceIn(1f, maxWidthPx)
        val userValue = ((rawOffsetPx / slideStepPx).roundToInt() * slideStepPx.roundToInt()).toFloat() / maxWidthPx

        return userValue.coerceIn(0f, 1f)
      }

      return rawOffsetPx / maxWidthPx
    }

    val draggableState = rememberDraggableState(
      onDelta = { delta ->
        rawOffsetInPx = (rawOffsetInPx + delta).coerceIn(0f, maxWidthPx)
        slideOffsetState.value = rawOffsetToUserValue(rawOffsetInPx, maxWidthPx)
        onValueChange(slideOffset)
      }
    )

    val gestureEndAction = rememberUpdatedState<(Float) -> Unit> {
      slideOffsetState.value = rawOffsetToUserValue(rawOffsetInPx, maxWidthPx)
      onValueChange(slideOffset)
    }

    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
      interactionSource.interactions.collect { interaction ->
        when (interaction) {
          is PressInteraction.Press -> interactions.add(interaction)
          is PressInteraction.Release -> interactions.remove(interaction.press)
          is PressInteraction.Cancel -> interactions.remove(interaction.press)
          is DragInteraction.Start -> interactions.add(interaction)
          is DragInteraction.Stop -> interactions.remove(interaction.start)
          is DragInteraction.Cancel -> interactions.remove(interaction.start)
        }
      }
    }

    val thumbRadius = if (interactions.isNotEmpty()) {
      thumbRadiusPressed
    } else {
      thumbRadiusNormal
    }

    val thumbColor = if (interactions.isNotEmpty()) {
      thumbColorNormal
    } else {
      thumbColorPressed
    }

    Canvas(
      modifier = Modifier
        .sliderPressModifier(
          draggableState = draggableState,
          interactionSource = interactionSource,
          maxPx = maxWidthPx,
          isRtl = false,
          rawOffset = rawOffsetState,
          gestureEndAction = gestureEndAction,
          enabled = true
        )
        .draggable(
          state = draggableState,
          orientation = Orientation.Horizontal,
          interactionSource = interactionSource,
          onDragStopped = { velocity -> gestureEndAction.value.invoke(velocity) }
        )
        .then(Modifier.fillMaxSize()),
      onDraw = {
        val centerY = size.height / 2f
        val thumbCenterY = (size.height + trackWidth) / 2f
        val halfRadius = thumbRadiusNormal / 2

        drawRect(
          color = trackColor,
          topLeft = Offset(0f, centerY),
          size = Size(size.width, trackWidth)
        )

        val positionX = (slideOffset * size.width)
          .coerceIn(halfRadius, size.width - halfRadius)

        drawCircle(
          color = thumbColor,
          radius = thumbRadius,
          center = Offset(x = positionX, y = thumbCenterY)
        )
      }
    )
  }
}

private fun Modifier.sliderPressModifier(
  draggableState: DraggableState,
  interactionSource: MutableInteractionSource,
  maxPx: Float,
  isRtl: Boolean,
  rawOffset: State<Float>,
  gestureEndAction: State<(Float) -> Unit>,
  enabled: Boolean
): Modifier {
  if (!enabled) {
    return this
  }

  return pointerInput(draggableState, interactionSource, maxPx, isRtl) {
    detectTapGestures(
      onPress = { pos ->
        draggableState.drag(MutatePriority.UserInput) {
          val to = if (isRtl) maxPx - pos.x else pos.x
          dragBy(to - rawOffset.value)
        }

        val interaction = PressInteraction.Press(pos)
        interactionSource.emit(interaction)

        val finishInteraction = try {
          val success = tryAwaitRelease()
          gestureEndAction.value.invoke(0f)

          if (success) {
            PressInteraction.Release(interaction)
          } else {
            PressInteraction.Cancel(interaction)
          }
        } catch (c: CancellationException) {
          PressInteraction.Cancel(interaction)
        }

        interactionSource.emit(finishInteraction)
      }
    )
  }
}