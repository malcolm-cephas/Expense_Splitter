package com.malcolm.expensesplitter.repositories;

import com.malcolm.expensesplitter.models.ExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, UUID> {
}
