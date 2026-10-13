package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.DistributionPreviewDto;
import com.malcolm.expensesplitter.models.Business;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.Investment;
import com.malcolm.expensesplitter.models.InvestmentTransactionType;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.BusinessRepository;
import com.malcolm.expensesplitter.repositories.InvestmentRepository;
import com.malcolm.expensesplitter.repositories.InvestmentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class InvestmentServiceTest {

    private InvestmentService service;
    private BusinessRepository businessRepository;
    private InvestmentRepository investmentRepository;
    private InvestmentTransactionRepository investmentTransactionRepository;

    @BeforeEach
    void setUp() {
        service = new InvestmentService();
        businessRepository = Mockito.mock(BusinessRepository.class);
        investmentRepository = Mockito.mock(InvestmentRepository.class);
        investmentTransactionRepository = Mockito.mock(InvestmentTransactionRepository.class);

        ReflectionTestUtils.setField(service, "businessRepository", businessRepository);
        ReflectionTestUtils.setField(service, "investmentRepository", investmentRepository);
        ReflectionTestUtils.setField(service, "investmentTransactionRepository", investmentTransactionRepository);

        Mockito.when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(investmentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void allCapitalOutstanding_shouldDistributeCapitalProportionallyWithZeroProfit() {
        Group group = new Group();
        Business business = new Business(group, "Business");
        User a = new User("A", "a@example.com", "INR");
        User b = new User("B", "b@example.com", "INR");
        User c = new User("C", "c@example.com", "INR");
        User d = new User("D", "d@example.com", "INR");

        addInvestment(business, a, new BigDecimal("450000"));
        addInvestment(business, b, new BigDecimal("400000"));
        addInvestment(business, c, new BigDecimal("75000"));
        addInvestment(business, d, new BigDecimal("75000"));

        DistributionPreviewDto preview = service.previewDistribution(business, new BigDecimal("150000"));

        assertEquals(new BigDecimal("150000.00"), preview.getDistributableAmount().setScale(2));
        assertEquals(new BigDecimal("150000.00"), preview.getTotalCapitalRecovery().setScale(2));
        assertEquals(new BigDecimal("0.00"), preview.getTotalProfit().setScale(2));
        assertEquals(List.of("A", "B", "C", "D"), preview.getLineItems().stream().map(item -> item.getInvestorName()).toList());

        assertEquals(new BigDecimal("67500.00"), preview.getLineItems().get(0).getCapitalRecovery());
        assertEquals(new BigDecimal("60000.00"), preview.getLineItems().get(1).getCapitalRecovery());
        assertEquals(new BigDecimal("11250.00"), preview.getLineItems().get(2).getCapitalRecovery());
        assertEquals(new BigDecimal("11250.00"), preview.getLineItems().get(3).getCapitalRecovery());

        assertEquals(BigDecimal.ZERO, preview.getLineItems().get(0).getProfit());
        assertEquals(BigDecimal.ZERO, preview.getLineItems().get(1).getProfit());
        assertEquals(BigDecimal.ZERO, preview.getLineItems().get(2).getProfit());
        assertEquals(BigDecimal.ZERO, preview.getLineItems().get(3).getProfit());
    }

    @Test
    void outstandingCapitalAndProfit_shouldReturnCapitalThenEqualProfitSplit() {
        Group group = new Group();
        Business business = new Business(group, "Business");
        UUID businessId = UUID.randomUUID();
        business.setId(businessId);
        User a = new User("A", "a@example.com", "INR");
        User b = new User("B", "b@example.com", "INR");
        User c = new User("C", "c@example.com", "INR");
        User d = new User("D", "d@example.com", "INR");

        addInvestment(business, a, new BigDecimal("25000"));
        addInvestment(business, b, new BigDecimal("25000"));
        addInvestment(business, c, new BigDecimal("0"));
        addInvestment(business, d, new BigDecimal("0"));

        Mockito.when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));

        DistributionPreviewDto preview = service.previewDistribution(business, new BigDecimal("150000"));

        assertEquals(new BigDecimal("50000.00"), preview.getTotalCapitalRecovery().setScale(2));
        assertEquals(new BigDecimal("100000.00"), preview.getTotalProfit().setScale(2));
        assertEquals(new BigDecimal("150000.00"), preview.getLineItems().stream()
                .map(DistributionPreviewDto.DistributionLineItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2));

        for (DistributionPreviewDto.DistributionLineItem item : preview.getLineItems()) {
            if (item.getInvestorName().equals("A") || item.getInvestorName().equals("B")) {
                assertEquals(new BigDecimal("25000.00"), item.getProfit());
            } else {
                assertEquals(new BigDecimal("25000.00"), item.getProfit());
            }
        }
    }

    @Test
    void confirmDistribution_shouldPersistImmutableLedgerTransactions() {
        Group group = new Group();
        Business business = new Business(group, "Business");
        business.setId(UUID.randomUUID());
        User investor = new User("A", "a@example.com", "INR");
        addInvestment(business, investor, new BigDecimal("100000"));

        Mockito.when(businessRepository.findById(business.getId())).thenReturn(Optional.of(business));

        DistributionPreviewDto preview = service.previewDistribution(business, new BigDecimal("15000"));
        DistributionPreviewDto confirmed = service.confirmDistribution(business, new BigDecimal("15000"));

        assertEquals(preview.getLineItems().size(), confirmed.getLineItems().size());
        assertEquals(new BigDecimal("15000.00"), confirmed.getTotalCapitalRecovery().setScale(2));

        assertFalse(business.getTransactions().isEmpty());
        assertTrue(business.getTransactions().stream().anyMatch(tx -> tx.getType() == InvestmentTransactionType.CAPITAL_RETURN));
    }

    private Investment addInvestment(Business business, User investor, BigDecimal originalInvestment) {
        Investment investment = new Investment(business, investor, originalInvestment);
        business.addInvestment(investment);
        return investment;
    }
}
