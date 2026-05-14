package com.financetracker.repository;

import com.financetracker.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUserIdOrderByDateDesc(Long userId);

    List<Income> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    Optional<Income> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND i.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT EXTRACT(MONTH FROM i.date) as month, SUM(i.amount) as total FROM income i WHERE i.user_id = :userId AND EXTRACT(YEAR FROM i.date) = :year GROUP BY EXTRACT(MONTH FROM i.date) ORDER BY month", nativeQuery = true)
    List<Object[]> findMonthlyIncomeSummaryNative(@Param("userId") Long userId, @Param("year") int year);

    @Query(value = "SELECT i.source, SUM(i.amount) as total FROM income i WHERE i.user_id = :userId AND i.date BETWEEN :startDate AND :endDate GROUP BY i.source ORDER BY total DESC", nativeQuery = true)
    List<Object[]> findSourceWiseSummaryNative(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
