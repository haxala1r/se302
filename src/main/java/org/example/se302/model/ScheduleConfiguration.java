package org.example.se302.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for exam schedule generation.
 * Defines the scheduling parameters and optimization strategy.
 * 
 * <h3>Configuration Parameters:</h3>
 * 
 * <pre>
 * ScheduleConfiguration = {
 *     numDays:              Number of exam days (e.g., 5)
 *     slotsPerDay:          Number of time slots per day (e.g., 4)
 *     startDate:            First day of exam period
 *     slotDuration:         Duration of each slot in minutes (e.g., 120)
 *     dayStartTime:         First slot start time (e.g., 09:00)
 *     optimizationStrategy: Strategy for optimization
 * }
 * </pre>
 */
public class ScheduleConfiguration {

    /**
     * Optimization strategies for the scheduling algorithm.
     */
    public enum OptimizationStrategy {
        /** Minimize total number of days used */
        MINIMIZE_DAYS,
        /** Spread exams evenly across days */
        BALANCED_DISTRIBUTION,
        /** Minimize consecutive exams for students */
        STUDENT_FRIENDLY,
        /** Maximize classroom utilization */
        MAXIMIZE_ROOM_USAGE,
        /** Default balanced approach */
        DEFAULT
    }

    // Core scheduling parameters
    private int numDays;
    private int slotsPerDay;
    private LocalDate startDate;

    // Time configuration
    private int slotDurationMinutes;
    private LocalTime dayStartTime;
    private int breakBetweenSlotsMinutes;

    // Optimization settings
    private OptimizationStrategy optimizationStrategy;
    private boolean allowBackToBackExams;
    private int maxExamsPerDay;

    // Algorithm parameters
    private long timeoutMs;
    private boolean useHeuristics;

    /**
     * Creates a default schedule configuration.
     */
    public ScheduleConfiguration() {
        this.numDays = 5;
        this.slotsPerDay = 4;
        this.startDate = LocalDate.now().plusDays(7); // Default: start in a week
        this.slotDurationMinutes = 120; // 2 hours
        this.dayStartTime = LocalTime.of(9, 0); // 09:00
        this.breakBetweenSlotsMinutes = 30; // 30 minute break
        this.optimizationStrategy = OptimizationStrategy.DEFAULT;
        this.allowBackToBackExams = true;
        this.maxExamsPerDay = 0; // 0 = no limit
        this.timeoutMs = 60000; // 60 seconds
        this.useHeuristics = true;
    }

    /**
     * Creates a schedule configuration with specified days and slots.
     * 
     * @param numDays     Number of exam days
     * @param slotsPerDay Number of time slots per day
     */
    public ScheduleConfiguration(int numDays, int slotsPerDay) {
        this();
        this.numDays = numDays;
        this.slotsPerDay = slotsPerDay;
    }

    /**
     * Creates a schedule configuration with all core parameters.
     * 
     * @param numDays              Number of exam days
     * @param slotsPerDay          Number of time slots per day
     * @param optimizationStrategy Strategy for optimization
     */
    public ScheduleConfiguration(int numDays, int slotsPerDay, OptimizationStrategy optimizationStrategy) {
        this(numDays, slotsPerDay);
        this.optimizationStrategy = optimizationStrategy;
    }

    /**
     * Gets the total number of available time slots across all days.
     */
    public int getTotalSlots() {
        return numDays * slotsPerDay;
    }

    /**
     * Generates a list of TimeSlot objects based on this configuration.
     * 
     * @return List of all available TimeSlots
     */
    public List<TimeSlot> generateTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();

        LocalDate currentDate = startDate;
        for (int day = 0; day < numDays; day++) {
            LocalTime currentTime = dayStartTime;

            for (int slot = 0; slot < slotsPerDay; slot++) {
                LocalTime endTime = currentTime.plusMinutes(slotDurationMinutes);
                timeSlots.add(new TimeSlot(currentDate, currentTime, endTime));

                // Move to next slot (add duration + break)
                currentTime = endTime.plusMinutes(breakBetweenSlotsMinutes);
            }

            // Move to next day
            currentDate = currentDate.plusDays(1);
        }

        return timeSlots;
    }

    /**
     * Gets the TimeSlot for a specific day and slot index.
     * 
     * @param day       Day index (0-based)
     * @param slotIndex Slot index within the day (0-based)
     * @return The corresponding TimeSlot
     */
    public TimeSlot getTimeSlot(int day, int slotIndex) {
        if (day < 0 || day >= numDays || slotIndex < 0 || slotIndex >= slotsPerDay) {
            return null;
        }

        LocalDate date = startDate.plusDays(day);
        LocalTime startTime = dayStartTime;

        // Calculate start time for this slot
        for (int i = 0; i < slotIndex; i++) {
            startTime = startTime.plusMinutes(slotDurationMinutes + breakBetweenSlotsMinutes);
        }

        LocalTime endTime = startTime.plusMinutes(slotDurationMinutes);
        return new TimeSlot(date, startTime, endTime);
    }

    /**
     * Converts a flat slot index to (day, slotIndex) pair.
     * 
     * @param flatIndex Flat index (0 to totalSlots-1)
     * @return Array of [day, slotIndex]
     */
    public int[] flatIndexToDaySlot(int flatIndex) {
        int day = flatIndex / slotsPerDay;
        int slot = flatIndex % slotsPerDay;
        return new int[] { day, slot };
    }

    /**
     * Converts (day, slotIndex) to a flat slot index.
     * 
     * @param day       Day index
     * @param slotIndex Slot index
     * @return Flat index
     */
    public int daySlotToFlatIndex(int day, int slotIndex) {
        return day * slotsPerDay + slotIndex;
    }

    /**
     * Gets a display name for a specific slot.
     * 
     * @param day       Day index (0-based)
     * @param slotIndex Slot index (0-based)
     * @return Display name like "Day 1 - 09:00-11:00"
     */
    public String getSlotDisplayName(int day, int slotIndex) {
        TimeSlot slot = getTimeSlot(day, slotIndex);
        if (slot == null) {
            return "Invalid Slot";
        }
        return String.format("Day %d - %s", day + 1, slot.getStartTime() + "-" + slot.getEndTime());
    }

    /**
     * Validates the configuration.
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        if (numDays <= 0) {
            return "Number of days must be positive";
        }
        if (slotsPerDay <= 0) {
            return "Slots per day must be positive";
        }
        if (slotDurationMinutes <= 0) {
            return "Slot duration must be positive";
        }
        if (startDate == null) {
            return "Start date is required";
        }
        if (dayStartTime == null) {
            return "Day start time is required";
        }
        if (timeoutMs <= 0) {
            return "Timeout must be positive";
        }
        return null; // Valid
    }

    // Getters and Setters

    public int getNumDays() {
        return numDays;
    }

    public void setNumDays(int numDays) {
        this.numDays = numDays;
    }

    public int getSlotsPerDay() {
        return slotsPerDay;
    }

    public void setSlotsPerDay(int slotsPerDay) {
        this.slotsPerDay = slotsPerDay;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public LocalTime getDayStartTime() {
        return dayStartTime;
    }

    public void setDayStartTime(LocalTime dayStartTime) {
        this.dayStartTime = dayStartTime;
    }

    public int getBreakBetweenSlotsMinutes() {
        return breakBetweenSlotsMinutes;
    }

    public void setBreakBetweenSlotsMinutes(int breakBetweenSlotsMinutes) {
        this.breakBetweenSlotsMinutes = breakBetweenSlotsMinutes;
    }

    public OptimizationStrategy getOptimizationStrategy() {
        return optimizationStrategy;
    }

    public void setOptimizationStrategy(OptimizationStrategy optimizationStrategy) {
        this.optimizationStrategy = optimizationStrategy;
    }

    public boolean isAllowBackToBackExams() {
        return allowBackToBackExams;
    }

    public void setAllowBackToBackExams(boolean allowBackToBackExams) {
        this.allowBackToBackExams = allowBackToBackExams;
    }

    public int getMaxExamsPerDay() {
        return maxExamsPerDay;
    }

    public void setMaxExamsPerDay(int maxExamsPerDay) {
        this.maxExamsPerDay = maxExamsPerDay;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isUseHeuristics() {
        return useHeuristics;
    }

    public void setUseHeuristics(boolean useHeuristics) {
        this.useHeuristics = useHeuristics;
    }

    @Override
    public String toString() {
        return String.format("ScheduleConfiguration[%d days, %d slots/day, strategy=%s, start=%s]",
                numDays, slotsPerDay, optimizationStrategy, startDate);
    }

    /**
     * Returns a detailed summary of the configuration.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Schedule Configuration ===\n");
        sb.append(String.format("Days: %d\n", numDays));
        sb.append(String.format("Slots per day: %d\n", slotsPerDay));
        sb.append(String.format("Total slots: %d\n", getTotalSlots()));
        sb.append(String.format("Start date: %s\n", startDate));
        sb.append(String.format("Day starts at: %s\n", dayStartTime));
        sb.append(String.format("Slot duration: %d minutes\n", slotDurationMinutes));
        sb.append(String.format("Break between slots: %d minutes\n", breakBetweenSlotsMinutes));
        sb.append(String.format("Optimization: %s\n", optimizationStrategy));
        sb.append(String.format("Allow back-to-back: %s\n", allowBackToBackExams));
        sb.append(String.format("Timeout: %d ms\n", timeoutMs));
        return sb.toString();
    }
}
