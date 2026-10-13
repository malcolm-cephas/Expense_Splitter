package com.malcolm.expensesplitter.repositories;

import com.malcolm.expensesplitter.models.Business;
import com.malcolm.expensesplitter.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {
    List<Business> findByGroupId(UUID groupId);
    List<Business> findByGroup(Group group);
}
