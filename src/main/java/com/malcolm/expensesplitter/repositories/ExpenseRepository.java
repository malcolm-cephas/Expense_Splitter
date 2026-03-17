package com.malcolm.expensesplitter.repositories;

import com.malcolm.expensesplitter.models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {
    List<Expense> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
