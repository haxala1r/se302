package org.example.se302.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a time slot for exam scheduling.
 * A time slot is defined by a date and a specific time period.
 */
public class TimeSlot implements Comparable<TimeSlot> {
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Creates a time slot with a default duration of 2 hours.
     */
    public TimeSlot(LocalDate date, LocalTime startTime) {
        this(date, startTime, startTime.plusHours(2));
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Checks if this time slot overlaps with another time slot.
     * Two time slots overlap if they are on the same day and their time ranges
     * intersect.
     */
    public boolean overlapsWith(TimeSlot other) {
        if (!this.date.equals(other.date)) {
            return false;
        }
        // Check if time ranges overlap
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    /**
     * Returns a unique identifier for this time slot.
     */
    public String getId() {
        return date.format(DATE_FORMATTER) + "_" + startTime.format(TIME_FORMATTER);
    }

    @Override
    public int compareTo(TimeSlot other) {
        int dateCompare = this.date.compareTo(other.date);
        if (dateCompare != 0) {
            return dateCompare;
        }
        return this.startTime.compareTo(other.startTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(date, timeSlot.date) &&
                Objects.equals(startTime, timeSlot.startTime) &&
                Objects.equals(endTime, timeSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, startTime, endTime);
    }

    @Override
    public String toString() {
        return date.format(DATE_FORMATTER) + " " +
                startTime.format(TIME_FORMATTER) + "-" +
                endTime.format(TIME_FORMATTER);
    }
}
