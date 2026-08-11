package com.libris.loan.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.libris.loan.LoanProperties;
import com.libris.testsupport.TestFixtures;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DueDateCalculatorTest {

    private final DueDateCalculator calculator = new DueDateCalculator(TestFixtures.defaultLoanProperties());

    @Test
    @DisplayName("la fecha límite es el préstamo más 14 días")
    void addsTheConfiguredLoanLength() {
        assertThat(calculator.dueDateFor(LocalDate.of(2026, 8, 11)))
                .isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    @DisplayName("cruza el fin de mes correctamente")
    void crossesMonthBoundaries() {
        assertThat(calculator.dueDateFor(LocalDate.of(2026, 1, 25)))
                .isEqualTo(LocalDate.of(2026, 2, 8));
    }

    @Test
    @DisplayName("respeta un plazo configurado distinto")
    void honoursConfiguration() {
        LoanProperties sevenDays = new LoanProperties(7, 3, 3, 2, 3, 90, 7);
        assertThat(new DueDateCalculator(sevenDays).dueDateFor(LocalDate.of(2026, 8, 11)))
                .isEqualTo(LocalDate.of(2026, 8, 18));
    }
}
