package com.financetracker.repository;

import com.financetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    List<Expense> findByUserIdAndCategoryIdOrderByDateDesc(Long userId, Long categoryId);

    List<Expense> findByUserIdAndDateBetweenAndCategoryIdOrderByDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate, Long categoryId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.category.id = :categoryId AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserIdAndCategoryIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.date BETWEEN :startDate AND :endDate GROUP BY e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> findCategoryWiseSummary(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MONTH(e.date), SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND YEAR(e.date) = :year GROUP BY MONTH(e.date) ORDER BY MONTH(e.date)")
    List<Object[]> findMonthlyExpenseSummary(@Param("userId") Long userId, @Param("year") int year);

    @Query(value = "SELECT EXTRACT(MONTH FROM e.date) as month, SUM(e.amount) as total FROM expenses e WHERE e.user_id = :userId AND EXTRACT(YEAR FROM e.date) = :year GROUP BY EXTRACT(MONTH FROM e.date) ORDER BY month", nativeQuery = true)
    List<Object[]> findMonthlyExpenseSummaryNative(@Param("userId") Long userId, @Param("year") int year);

    @Query(value = "SELECT c.name, SUM(e.amount) as total FROM expenses e JOIN categories c ON e.category_id = c.id WHERE e.user_id = :userId AND e.date BETWEEN :startDate AND :endDate GROUP BY c.name ORDER BY total DESC", nativeQuery = true)
    List<Object[]> findCategoryWiseSummaryNative(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.id = :userId AND e.date BETWEEN :startDate AND :endDate")
    long countByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
