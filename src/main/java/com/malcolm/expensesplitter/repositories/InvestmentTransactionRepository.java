package com.malcolm.expensesplitter.repositories;

import com.malcolm.expensesplitter.models.Business;
import com.malcolm.expensesplitter.models.InvestmentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, UUID> {
    List<InvestmentTransaction> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    List<InvestmentTransaction> findByBusiness(Business business);
}
