package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.data.model.Category
import com.example.ui.utils.getIconByName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date


/**
 * Primary central button for the application, following Material 3 guidelines and app theme dimensions.
 */
@Composable
fun FinanceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: Any? = null, // Can be ImageVector or Painter
    iconTint: Color = Color.Unspecified,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = AppShapes.roundedButton,
    height: Dp = AppDimens.heightButton,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    when (icon) {
                        is ImageVector -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is Painter -> {
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Secondary central outlined button for the application.
 */
@Composable
fun FinanceOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Any? = null,
    iconTint: Color = Color.Unspecified,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shape: Shape = AppShapes.roundedButton,
    height: Dp = AppDimens.heightButton,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = BorderStroke(AppDimens.borderWidthThin, if (enabled) borderColor else borderColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reusable simple text button for less prominent actions.
 */
@Composable
fun FinanceTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    icon: Any? = null,
    iconTint: Color = Color.Unspecified,
    testTag: String? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    is androidx.compose.ui.graphics.painter.Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight
            )
        }
    }
}

/**
 * Unified Icon Button component that ensures a minimum touch target size of 48dp x 48dp.
 */
@Composable
fun FinanceIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    size: Dp = 24.dp,
    testTag: String? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // Minimum touch target size
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Consistent segment button for switching states (e.g., Income vs Expense).
 */
@Composable
fun FinanceSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = CircleShape,
    testTag: String? = null
) {
    val containerColor = if (selected) selectedContainerColor else unselectedContainerColor
    val contentColor = if (selected) selectedContentColor else unselectedContentColor

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(containerColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = contentColor)
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Unified Finance Card with centralized design values.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = AppShapes.roundedCardMedium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 1.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            content = content
        )
    }
}

/**
 * Modern Segmented Control for selecting Time Horizon (Day, Week, Month, Year).
 */
@Composable
fun TimePeriodSelector(
    selectedPeriod: com.example.ui.viewmodel.TimePeriod,
    onPeriodSelected: (com.example.ui.viewmodel.TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.viewmodel.TimePeriod.values().forEach { period ->
                val isSelected = selectedPeriod == period
                val backgroundAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "bgAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha)
                            else Color.Transparent
                        )
                        .clickable { onPeriodSelected(period) }
                        .testTag("time_period_${period.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Premium Period Navigator for jumping between days/weeks/months/years.
 */
@Composable
fun PeriodNavigator(
    periodLabel: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
    isNextEnabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = AppDimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .testTag("period_nav_prev")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Period",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        val labelModifier = if (onLabelClick != null) {
            Modifier
                .weight(1f)
                .padding(horizontal = AppDimens.paddingSmall)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLabelClick() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(vertical = 6.dp, horizontal = 12.dp)
                .testTag("period_nav_label_clickable")
        } else {
            Modifier
                .weight(1f)
                .padding(horizontal = AppDimens.paddingSmall)
                .testTag("period_nav_label")
        }

        Row(
            modifier = labelModifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (onLabelClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(
            onClick = onNextClick,
            enabled = isNextEnabled,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isNextEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .testTag("period_nav_next")
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Period",
                tint = if (isNextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Premium Custom Period Picker Dialog for direct date/year selection.
 */
@Composable
fun CustomPeriodPickerDialog(
    timePeriod: com.example.ui.viewmodel.TimePeriod,
    activeDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.roundedCardLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal)
                .testTag("period_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(AppDimens.paddingNormal)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (timePeriod) {
                        com.example.ui.viewmodel.TimePeriod.DAY -> "Select Date"
                        com.example.ui.viewmodel.TimePeriod.WEEK -> "Select Week"
                        com.example.ui.viewmodel.TimePeriod.MONTH -> "Select Month & Year"
                        com.example.ui.viewmodel.TimePeriod.YEAR -> "Select Year"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = AppDimens.paddingNormal)
                )

                when (timePeriod) {
                    com.example.ui.viewmodel.TimePeriod.DAY,
                    com.example.ui.viewmodel.TimePeriod.WEEK -> {
                        DayOrWeekPickerContent(
                            initialDate = activeDate,
                            isWeekPicker = (timePeriod == com.example.ui.viewmodel.TimePeriod.WEEK),
                            onDateSelected = {
                                onDateSelected(it)
                                onDismiss()
                            }
                        )
                    }
                    com.example.ui.viewmodel.TimePeriod.MONTH -> {
                        MonthPickerContent(
                            initialDate = activeDate,
                            onMonthSelected = {
                                onDateSelected(it)
                                onDismiss()
                            }
                        )
                    }
                    com.example.ui.viewmodel.TimePeriod.YEAR -> {
                        YearPickerContent(
                            initialDate = activeDate,
                            onYearSelected = {
                                onDateSelected(it)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FinanceTextButton(
                        text = "Cancel",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun YearPickerContent(
    initialDate: Long,
    onYearSelected: (Long) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 5)..(currentYear + 5)
    val selectedYear = Calendar.getInstance().apply { timeInMillis = initialDate }.get(Calendar.YEAR)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in years.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (year in row) {
                    val isSelected = year == selectedYear
                    val isFutureYear = year > currentYear
                    Box(
                        modifier = if (isFutureYear) {
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                        } else {
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = initialDate
                                        set(Calendar.YEAR, year)
                                    }
                                    onYearSelected(cal.timeInMillis)
                                }
                                .padding(4.dp)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isFutureYear -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthPickerContent(
    initialDate: Long,
    onMonthSelected: (Long) -> Unit
) {
    var calendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialDate })
    }
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)

    val realCal = Calendar.getInstance()
    val realYear = realCal.get(Calendar.YEAR)
    val realMonth = realCal.get(Calendar.MONTH)

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Year Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis
                    add(Calendar.YEAR, -1)
                }
                calendar = newCal
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
            }
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.YEAR, 1)
                    }
                    calendar = newCal
                },
                enabled = currentYear < realYear
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Year",
                    tint = if (currentYear < realYear) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Months 3x4 Grid
        for (rowIdx in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIdx in 0..2) {
                    val monthIdx = rowIdx * 3 + colIdx
                    val isSelected = monthIdx == currentMonth
                    val monthName = monthNames[monthIdx]
                    val isMonthFuture = (currentYear > realYear) || (currentYear == realYear && monthIdx > realMonth)

                    Box(
                        modifier = if (isMonthFuture) {
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .padding(vertical = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        } else {
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .padding(vertical = 4.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val selectedCal = Calendar.getInstance().apply {
                                        timeInMillis = calendar.timeInMillis
                                        set(Calendar.MONTH, monthIdx)
                                    }
                                    onMonthSelected(selectedCal.timeInMillis)
                                }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isMonthFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayOrWeekPickerContent(
    initialDate: Long,
    isWeekPicker: Boolean,
    onDateSelected: (Long) -> Unit
) {
    var calendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialDate })
    }
    
    val selectedCal = Calendar.getInstance().apply { timeInMillis = initialDate }
    
    // Calculate the start and end of week if isWeekPicker
    val weekStart = remember(initialDate) {
        Calendar.getInstance().apply {
            timeInMillis = initialDate
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val weekEnd = remember(initialDate) {
        Calendar.getInstance().apply {
            timeInMillis = weekStart
            add(Calendar.DATE, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val monthYearLabel = remember(calendar) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(calendar.time)
    }

    val realCal = Calendar.getInstance()
    val realYear = realCal.get(Calendar.YEAR)
    val realMonth = realCal.get(Calendar.MONTH)
    val canGoToNextMonth = calendar.get(Calendar.YEAR) < realYear || 
            (calendar.get(Calendar.YEAR) == realYear && calendar.get(Calendar.MONTH) < realMonth)

    val todayMaxCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    // Get calendar days for the active month
    val daysList = remember(calendar) {
        val list = mutableListOf<Calendar?>()
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Days before month start (padding)
        val firstDayOfWeekIndex = tempCal.get(Calendar.DAY_OF_WEEK)
        val paddingDays = firstDayOfWeekIndex - tempCal.firstDayOfWeek
        val adjustedPadding = if (paddingDays < 0) paddingDays + 7 else paddingDays
        
        for (i in 0 until adjustedPadding) {
            list.add(null)
        }
        
        // Days of current month
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                set(Calendar.DAY_OF_MONTH, day)
            }
            list.add(dayCal)
        }
        
        // Padding at the end to make complete rows of 7
        while (list.size % 7 != 0) {
            list.add(null)
        }
        list
    }

    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Month/Year navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis
                    add(Calendar.MONTH, -1)
                }
                calendar = newCal
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            Text(
                text = monthYearLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                        add(Calendar.MONTH, 1)
                    }
                    calendar = newCal
                },
                enabled = canGoToNextMonth
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = if (canGoToNextMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            for (day in weekdays) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Days grid
        for (row in daysList.chunked(7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (dayCal in row) {
                    if (dayCal != null) {
                        val isDaySelected = !isWeekPicker && 
                                dayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                dayCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                        val isDayInSelectedWeek = isWeekPicker && 
                                dayCal.timeInMillis in weekStart..weekEnd

                        val isFutureDay = dayCal.timeInMillis > todayMaxCal.timeInMillis

                        val backgroundColor = when {
                            isDaySelected -> MaterialTheme.colorScheme.primary
                            isDayInSelectedWeek -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isFutureDay -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            isDaySelected -> MaterialTheme.colorScheme.onPrimary
                            isDayInSelectedWeek -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = if (isFutureDay) {
                                Modifier.size(36.dp)
                            } else {
                                Modifier
                                    .size(36.dp)
                                    .background(backgroundColor, shape = CircleShape)
                                    .clickable {
                                        onDateSelected(dayCal.timeInMillis)
                                    }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isDaySelected || isDayInSelectedWeek) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}


/**
 * A reusable flow-layout grid of category chips that can be used in both the
 * Manage Categories screen and the Filter sheet.
 *
 * @param categories The full list of [Category] objects to display as chips.
 * @param selectedIds The set of category [Category.id] values that are currently selected
 *   (highlighted). Pass an empty set when no selection state is needed.
 * @param onToggle Callback invoked with the [Category.id] of the chip the user tapped.
 * @param onLongPress Optional callback invoked with the [Category.id] of the chip the user
 *   long-pressed. When non-null, each chip becomes long-pressable (used by
 *   CategoryManagementScreen to surface an Edit / Delete menu).
 * @param modifier Optional [Modifier] applied to the root [FlowRow].
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryChipGrid(
    categories: List<Category>,
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onLongPress: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedIds.contains(category.id)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(category.id) },
                label = { Text(category.name) },
                leadingIcon = {
                    Icon(
                        imageVector = getIconByName(category.iconName),
                        contentDescription = category.name,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = if (onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = { onToggle(category.id) },
                        onLongClick = { onLongPress(category.id) }
                    )
                } else Modifier
            )
        }
    }
}


/**
 * A unified profile avatar that consistently renders user initials or guest identifiers.
 * Follows core theme guidelines and supports scale adjustments and tap actions.
 *
 * @param name The display name of the user (e.g. "Alex Mitchell" or "Guest User").
 * @param isGuest Indicates if the active session is a guest session.
 * @param size The physical diameter size of the circular avatar.
 * @param modifier Additional modifiers to apply to the avatar layout.
 * @param onClick Optional click action. If provided, the avatar behaves as a button.
 */
@Composable
fun ProfileAvatar(
    name: String?,
    isGuest: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 1. Unify Initials Generation Logic
    val initials = remember(name, isGuest) {
        if (name.isNullOrBlank()) {
            if (isGuest) "GU" else "U"
        } else {
            val parts = name.trim().split("\\s+".toRegex())
            val rawInitials = if (parts.size >= 2) {
                (parts[0].firstOrNull()?.toString() ?: "") + (parts[1].firstOrNull()?.toString() ?: "")
            } else {
                parts[0].firstOrNull()?.toString() ?: ""
            }
            rawInitials.uppercase().take(2).ifEmpty { if (isGuest) "GU" else "U" }
        }
    }

    // 2. Unify Color Styling Tokens
    val backgroundColor = if (isGuest) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    
    val textColor = if (isGuest) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    val borderColor = if (isGuest) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }

    // 3. Define the Core Circular Composable
    val avatarContent = @Composable {
        Box(
            modifier = Modifier
                .size(size)
                .background(backgroundColor, CircleShape)
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = when {
                    size < 40.dp -> MaterialTheme.typography.labelMedium
                    size < 55.dp -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleLarge
                }
            )
        }
    }

    // 4. Handle Click Actions Consistent with M3 Targets
    if (onClick != null) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(size)
        ) {
            avatarContent()
        }
    } else {
        Box(modifier = modifier) {
            avatarContent()
        }
    }
}



@Composable
fun TransactionItem(
    transaction: com.example.data.model.TransactionWithCategory,
    modifier: Modifier = Modifier,
    verticalPadding: androidx.compose.ui.unit.Dp = 4.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
) {
    val tx = transaction.transaction
    val category = transaction.category
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val isExpense = tx.type == com.example.data.model.TransactionType.EXPENSE
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isExpense) ExpenseRed.copy(alpha = 0.8f) else IncomeGreen.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = com.example.ui.utils.getIconByName(category?.iconName ?: "category"),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.source.ifBlank { category?.name ?: "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${category?.name ?: "Unknown"} • ${dateFormat.format(Date(tx.date))} ${timeFormat.format(Date(tx.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${if (isExpense) "-₹" else "+₹"}${com.example.ui.utils.CurrencyUtils.formatRupees(tx.amount).replace("₹", "").trim()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) ExpenseRed else IncomeGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    transactionWithCat: com.example.data.model.TransactionWithCategory,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val tx = transactionWithCat.transaction
    val cat = transactionWithCat.category
    val isExpense = tx.type == com.example.data.model.TransactionType.EXPENSE
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = if (isExpense) Color(0xFFFDE9E9) else Color(0xFFE9FDF0),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = com.example.ui.utils.getIconByName(cat?.iconName ?: "category"),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = com.example.ui.utils.CurrencyUtils.formatRupees(tx.amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tx.source.ifBlank { cat?.name ?: "Unknown" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (tx.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tx.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onEdit() }) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.padding(12.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDuplicate() }) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.padding(12.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Duplicate", style = MaterialTheme.typography.labelMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDelete() }) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * A reusable empty state placeholder displaying an app watermark and message.
 */
@Composable
fun EmptyStatePlaceholder(
    message: String = "No transactions yet",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = com.example.R.drawable.ic_app_logo_content),
            contentDescription = "Empty State Logo",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

