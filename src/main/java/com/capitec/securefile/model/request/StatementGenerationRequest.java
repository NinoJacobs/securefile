package com.capitec.securefile.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
public class StatementGenerationRequest {

    private static final long MAX_CUSTOM_RANGE_DAYS = 366;

    @NotNull
    private StatementPeriod period = StatementPeriod.ONE_MONTH;

    @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @AssertTrue(message = "startDate and endDate are required for custom statements")
    public boolean isCustomDateRangePresent() {
        return period != StatementPeriod.CUSTOM || startDate != null && endDate != null;
    }

    @AssertTrue(message = "startDate must be on or before endDate")
    public boolean isDateRangeOrdered() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }

    @AssertTrue(message = "custom statement range cannot be longer than 366 days")
    public boolean isCustomDateRangeWithinLimit() {
        return period != StatementPeriod.CUSTOM
                || startDate == null
                || endDate == null
                || ChronoUnit.DAYS.between(startDate, endDate) <= MAX_CUSTOM_RANGE_DAYS;
    }
}
