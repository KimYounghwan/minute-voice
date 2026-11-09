package kr.co.minutevoice;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Published Korean calendar dates; sources and coverage are recorded in README.md. */
public final class KoreanHolidays {
    private static final Set<String> DATES = new HashSet<>(Arrays.asList(
        "2026-01-01", "2026-02-16", "2026-02-17", "2026-02-18",
        "2026-03-01", "2026-03-02", "2026-05-01", "2026-05-05",
        "2026-05-24", "2026-05-25", "2026-06-03", "2026-06-06",
        "2026-07-17", "2026-08-15", "2026-08-17", "2026-09-24",
        "2026-09-25", "2026-09-26", "2026-10-03", "2026-10-05",
        "2026-10-09", "2026-12-25",
        "2027-01-01", "2027-02-06", "2027-02-07", "2027-02-08", "2027-02-09",
        "2027-03-01", "2027-05-01", "2027-05-03", "2027-05-05", "2027-05-13",
        "2027-06-06", "2027-07-17", "2027-07-19", "2027-08-15", "2027-08-16",
        "2027-09-14", "2027-09-15", "2027-09-16", "2027-10-03", "2027-10-04",
        "2027-10-09", "2027-10-11", "2027-12-25", "2027-12-27"
    ));
    public static boolean supported(LocalDate date) { return date.getYear() >= 2026 && date.getYear() <= 2027; }
    // Ordinary Sundays are controlled by the weekday selection, not this option.
    public static boolean isHoliday(LocalDate date) { return DATES.contains(date.toString()); }
}
