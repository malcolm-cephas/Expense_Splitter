package com.malcolm.expensesplitter.repositories;

import com.malcolm.expensesplitter.models.Business;
import com.malcolm.expensesplitter.models.Investment;
import com.malcolm.expensesplitter.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    List<Investment> findByBusinessId(UUID businessId);
    List<Investment> findByBusiness(Business business);
    Optional<Investment> findByBusinessAndInvestor(Business business, User investor);
}
