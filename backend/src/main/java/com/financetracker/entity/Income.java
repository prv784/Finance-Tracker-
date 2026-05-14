package com.financetracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "income")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncomeSource source = IncomeSource.OTHER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String notes;

    @Column
    @Builder.Default
    private boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column
    private RecurrenceType recurrenceType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum IncomeSource {
        SALARY, FREELANCE, BUSINESS, INVESTMENT, RENTAL, GIFT, BONUS, OTHER
    }

    public enum RecurrenceType {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}
