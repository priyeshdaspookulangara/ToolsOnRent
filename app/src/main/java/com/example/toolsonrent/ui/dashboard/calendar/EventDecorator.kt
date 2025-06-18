package com.example.toolsonrent.ui.dashboard.calendar

// android.graphics.Color might be needed if you were to use Color.parseColor or predefined Color constants directly.
// However, typically the color Int is passed from ContextCompat.getColor(context, R.color.your_color).
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.HashSet // Explicit import for HashSet

/**
 * A DayViewDecorator to add a colored dot below specific dates on the MaterialCalendarView.
 *
 * @param color The color of the dot (resolved color integer).
 * @param dates A collection of {@link CalendarDay} objects to be decorated.
 */
class EventDecorator(
    private val color: Int,
    dates: Collection<CalendarDay> // Constructor accepts a Collection for flexibility.
) : DayViewDecorator {

    // Internally, dates are stored in a HashSet for efficient 'contains' checks.
    private val datesToDecorate: HashSet<CalendarDay> = HashSet(dates)

    /**
     * Determines if a given {@link CalendarDay} should be decorated.
     * @param day The date to check.
     * @return True if the day is in the set of dates to decorate, false otherwise.
     */
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return datesToDecorate.contains(day)
    }

    /**
     * Applies the decoration (a dot span) to the {@link DayViewFacade}.
     * @param view The DayViewFacade to which the decoration should be applied.
     */
    override fun decorate(view: DayViewFacade) {
        // Add a dot span with a specified radius and color.
        // The radius (e.g., 7f) can be adjusted for visual preference.
        view.addSpan(DotSpan(7f, color))
        // To add multiple dots or more complex spans, this method can be extended.
    }

    // Optional: A method to update the dates if the decorator instance is long-lived and reused.
    // This is useful if you want to avoid creating new Decorator objects frequently.
    // fun updateDates(newDates: Collection<CalendarDay>) {
    //     datesToDecorate.clear()
    //     datesToDecorate.addAll(newDates)
    //     // After updating dates, the calendar view would need to be invalidated
    //     // or have decorators re-applied for changes to take effect.
    // }
}
