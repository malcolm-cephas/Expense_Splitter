package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.dto.DistributionPreviewDto;
import com.malcolm.expensesplitter.models.Business;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.Investment;
import com.malcolm.expensesplitter.models.InvestmentTransaction;
import com.malcolm.expensesplitter.models.InvestmentTransactionType;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.BusinessRepository;
import com.malcolm.expensesplitter.repositories.InvestmentRepository;
import com.malcolm.expensesplitter.repositories.InvestmentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InvestmentService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private InvestmentTransactionRepository investmentTransactionRepository;

    /**
     * Records money received from a member as capital contributed to the group's
     * investment/business. A single default business is used by the investment
     * mode UI, while the underlying model still supports multiple businesses.
     */
    @Transactional
    public InvestmentTransaction recordIncome(Group group, User investor, BigDecimal amount, String description) {
        return recordIncome(group, investor, amount, null, description);
    }

    @Transactional
    public InvestmentTransaction recordIncome(Group group, User investor, BigDecimal amount,
            BigDecimal expectedShare, String description) {
        if (group == null || investor == null) {
            throw new IllegalArgumentException("Group and member are required");
        }
        BigDecimal contribution = money(amount);
        if (contribution.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Income cannot be negative");
        }
        if (expectedShare != null && expectedShare.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Share cannot be negative");
        }

        Business business = getOrCreateDefaultBusiness(group);
        Investment investment = investmentRepository.findByBusinessAndInvestor(business, investor)
                .orElseGet(() -> new Investment(business, investor, BigDecimal.ZERO));
        boolean newInvestment = investment.getId() == null;
        investment.setOriginalInvestment(investment.getOriginalInvestment().add(contribution));
        if (expectedShare != null) {
            investment.setExpectedShare(money(expectedShare));
        }
        if (newInvestment) {
            business.addInvestment(investment);
        }
        investmentRepository.save(investment);

        InvestmentTransaction transaction = null;
        if (contribution.compareTo(BigDecimal.ZERO) > 0) {
            transaction = new InvestmentTransaction(
                    business, investor, InvestmentTransactionType.CAPITAL_CONTRIBUTION,
                    contribution, nonBlank(description, "Income / capital contribution"));
            business.addTransaction(transaction);
            investmentTransactionRepository.save(transaction);
        }
        businessRepository.save(business);
        return transaction;
    }

    /** Returns contribution - expected share - reimbursements for each member. */
    public Map<UUID, BigDecimal> calculateNetBalances(Group group) {
        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        if (group == null) {
            return balances;
        }
        businessRepository.findByGroup(group).stream()
                .flatMap(business -> business.getInvestments().stream())
                .forEach(investment -> {
                    if (investment.getInvestor() != null) {
                        BigDecimal balance = investment.getOriginalInvestment()
                                .subtract(investment.getExpectedShare())
                                .subtract(investment.getCapitalReturned());
                        balances.merge(investment.getInvestor().getId(), balance, BigDecimal::add);
                    }
                });
        return balances;
    }

    /** Records a targeted reimbursement, such as returning Malcolm's excess payment. */
    @Transactional
    public InvestmentTransaction reimburse(Group group, User investor, BigDecimal amount, String description) {
        if (group == null || investor == null) {
            throw new IllegalArgumentException("Group and member are required");
        }
        BigDecimal reimbursement = money(amount);
        if (reimbursement.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reimbursement must be greater than zero");
        }

        Business business = getOrCreateDefaultBusiness(group);
        Investment investment = investmentRepository.findByBusinessAndInvestor(business, investor)
                .orElseThrow(() -> new IllegalArgumentException("No contribution found for " + investor.getName()));
        if (reimbursement.compareTo(calculateOutstandingCapital(investment)) > 0) {
            throw new IllegalArgumentException("Reimbursement cannot exceed the member's outstanding contribution");
        }

        InvestmentTransaction transaction = new InvestmentTransaction(
                business, investor, InvestmentTransactionType.CAPITAL_RETURN,
                reimbursement, nonBlank(description, "Reimbursement"));
        business.addTransaction(transaction);
        investment.setCapitalReturned(investment.getCapitalReturned().add(reimbursement));
        investmentTransactionRepository.save(transaction);
        investmentRepository.save(investment);
        businessRepository.save(business);
        return transaction;
    }

    public Business getOrCreateDefaultBusiness(Group group) {
        return businessRepository.findByGroup(group).stream().findFirst().orElseGet(() -> {
            Business business = new Business(group, "Group Investment");
            business.setDescription("Income, contributions and reimbursements for this group");
            group.addBusiness(business);
            return businessRepository.save(business);
        });
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public DistributionPreviewDto previewDistribution(Business business, BigDecimal distributableAmount) {
        DistributionPreviewDto preview = new DistributionPreviewDto();
        BigDecimal amount = distributableAmount == null ? BigDecimal.ZERO : distributableAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        preview.setDistributableAmount(amount);
        preview.setTotalCapitalRecovery(BigDecimal.ZERO);
        preview.setTotalProfit(BigDecimal.ZERO);
        preview.setTotalOutstandingCapital(BigDecimal.ZERO);

        if (business == null || business.getInvestments() == null || business.getInvestments().isEmpty()) {
            return preview;
        }

        List<Investment> investments = new ArrayList<>(business.getInvestments());
        investments.sort(Comparator.comparing(i -> i.getInvestor() != null ? i.getInvestor().getName() : ""));

        BigDecimal totalOutstanding = investments.stream()
                .map(this::calculateOutstandingCapital)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        preview.setTotalOutstandingCapital(totalOutstanding);

        BigDecimal capitalRecoveryAvailable = amount.min(totalOutstanding);
        BigDecimal profitAvailable = amount.subtract(capitalRecoveryAvailable);

        MapWithRounds capitalAllocation = new MapWithRounds();
        MapWithRounds profitAllocation = new MapWithRounds();

        if (capitalRecoveryAvailable.compareTo(BigDecimal.ZERO) > 0) {
            capitalAllocation = allocateProportionally(investments, totalOutstanding, capitalRecoveryAvailable);
        }
        if (profitAvailable.compareTo(BigDecimal.ZERO) > 0) {
            profitAllocation = allocateEqualProfitShare(investments, profitAvailable);
        }

        for (Investment investment : investments) {
            BigDecimal capitalRecovery = capitalAllocation.getAmountFor(investment);
            BigDecimal profit = profitAllocation.getAmountFor(investment);
            BigDecimal total = capitalRecovery.add(profit).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            preview.getLineItems().add(new DistributionPreviewDto.DistributionLineItem(
                    investment.getInvestor(), capitalRecovery, profit, total));
            preview.setTotalCapitalRecovery(preview.getTotalCapitalRecovery().add(capitalRecovery));
            preview.setTotalProfit(preview.getTotalProfit().add(profit));
        }

        BigDecimal finalTotal = preview.getLineItems().stream()
                .map(DistributionPreviewDto.DistributionLineItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (finalTotal.compareTo(amount) != 0) {
            adjustForRemainder(preview, amount);
        }
        preview.setTotalCapitalRecovery(preview.getLineItems().stream()
                .map(DistributionPreviewDto.DistributionLineItem::getCapitalRecovery)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        preview.setTotalProfit(preview.getLineItems().stream()
                .map(DistributionPreviewDto.DistributionLineItem::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        preview.getLineItems().forEach(item -> {
            item.setTotal(item.getCapitalRecovery().add(item.getProfit()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        });
        return preview;
    }

    @Transactional
    public DistributionPreviewDto confirmDistribution(Business business, BigDecimal distributableAmount) {
        if (business == null) {
            throw new IllegalArgumentException("Business is required");
        }

        Business managedBusiness = businessRepository.findById(business.getId()).orElse(business);
        DistributionPreviewDto preview = previewDistribution(managedBusiness, distributableAmount);
        if (preview.getLineItems().isEmpty()) {
            return preview;
        }

        for (int index = 0; index < preview.getLineItems().size(); index++) {
            DistributionPreviewDto.DistributionLineItem item = preview.getLineItems().get(index);
            Investment investment = resolveInvestment(managedBusiness, item, index);
            if (investment == null) {
                continue;
            }

            BigDecimal capitalRecovery = item.getCapitalRecovery() == null ? BigDecimal.ZERO : item.getCapitalRecovery();
            BigDecimal profit = item.getProfit() == null ? BigDecimal.ZERO : item.getProfit();

            if (capitalRecovery.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newCapitalReturned = investment.getCapitalReturned().add(capitalRecovery);
                investment.setCapitalReturned(newCapitalReturned);
                InvestmentTransaction capitalTransaction = new InvestmentTransaction(
                        managedBusiness,
                        investment.getInvestor(),
                        InvestmentTransactionType.CAPITAL_RETURN,
                        capitalRecovery,
                        "Capital recovery distribution"
                );
                managedBusiness.addTransaction(capitalTransaction);
                investmentTransactionRepository.save(capitalTransaction);
            }

            if (profit.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newProfitReceived = investment.getProfitReceived().add(profit);
                investment.setProfitReceived(newProfitReceived);
                InvestmentTransaction profitTransaction = new InvestmentTransaction(
                        managedBusiness,
                        investment.getInvestor(),
                        InvestmentTransactionType.PROFIT_DISTRIBUTION,
                        profit,
                        "Profit distribution"
                );
                managedBusiness.addTransaction(profitTransaction);
                investmentTransactionRepository.save(profitTransaction);
            }

            investmentRepository.save(investment);
        }

        businessRepository.save(managedBusiness);
        return preview;
    }

    private BigDecimal calculateOutstandingCapital(Investment investment) {
        if (investment == null) {
            return BigDecimal.ZERO;
        }
        return investment.getOriginalInvestment().subtract(investment.getCapitalReturned());
    }

    private Investment resolveInvestment(Business business, DistributionPreviewDto.DistributionLineItem item, int index) {
        if (business == null || business.getInvestments() == null) {
            return null;
        }
        if (item != null && item.getInvestorId() != null) {
            return business.getInvestments().stream()
                    .filter(candidate -> candidate.getInvestor() != null && Objects.equals(candidate.getInvestor().getId(), item.getInvestorId()))
                    .findFirst()
                    .orElse(null);
        }
        return business.getInvestments().size() > index ? business.getInvestments().get(index) : null;
    }

    private MapWithRounds allocateProportionally(List<Investment> investments, BigDecimal totalOutstanding, BigDecimal totalAmount) {
        MapWithRounds map = new MapWithRounds();
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0 || totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            return map;
        }

        List<RatioEntry> entries = new ArrayList<>();
        BigDecimal allocatedTotal = BigDecimal.ZERO;
        for (Investment investment : investments) {
            BigDecimal outstanding = calculateOutstandingCapital(investment);
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                map.put(investment, BigDecimal.ZERO);
                continue;
            }
            BigDecimal exactAllocation = totalAmount.multiply(outstanding).divide(totalOutstanding, 10, RoundingMode.HALF_UP);
            BigDecimal whole = exactAllocation.setScale(MONEY_SCALE, RoundingMode.DOWN);
            BigDecimal remainder = exactAllocation.subtract(whole).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            entries.add(new RatioEntry(investment, exactAllocation, whole, remainder));
            map.put(investment, whole);
            allocatedTotal = allocatedTotal.add(whole);
        }

        long remainderInCents = totalAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(MONEY_SCALE)
                .longValue() - allocatedTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(MONEY_SCALE)
                .longValue();

        while (remainderInCents > 0) {
            RatioEntry selected = entries.stream()
                    .filter(entry -> entry.investment != null)
                    .max(Comparator.comparing(entry -> entry.remainder))
                    .orElse(null);
            if (selected == null) {
                break;
            }
            BigDecimal current = map.get(selected.investment);
            map.put(selected.investment, current.add(ONE_CENT));
            selected.remainder = BigDecimal.ZERO;
            remainderInCents--;
        }

        return map;
    }

    private MapWithRounds allocateEqualProfitShare(List<Investment> investments, BigDecimal totalAmount) {
        MapWithRounds map = new MapWithRounds();
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0 || investments.isEmpty()) {
            return map;
        }

        List<RatioEntry> entries = new ArrayList<>();
        int investorCount = investments.size();
        BigDecimal equalShare = totalAmount.divide(new BigDecimal(investorCount), 10, RoundingMode.HALF_UP);
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (Investment investment : investments) {
            BigDecimal whole = equalShare.setScale(MONEY_SCALE, RoundingMode.DOWN);
            BigDecimal remainder = equalShare.subtract(whole).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            entries.add(new RatioEntry(investment, equalShare, whole, remainder));
            map.put(investment, whole);
            runningTotal = runningTotal.add(whole);
        }

        long remainderInCents = totalAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(MONEY_SCALE)
                .longValue() - runningTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(MONEY_SCALE)
                .longValue();

        while (remainderInCents > 0) {
            RatioEntry selected = entries.stream()
                    .max(Comparator.comparing(entry -> entry.remainder))
                    .orElse(null);
            if (selected == null) {
                break;
            }
            BigDecimal current = map.get(selected.investment);
            map.put(selected.investment, current.add(ONE_CENT));
            selected.remainder = BigDecimal.ZERO;
            remainderInCents--;
        }

        return map;
    }

    private void adjustForRemainder(DistributionPreviewDto preview, BigDecimal distributableAmount) {
        BigDecimal expectedTotal = preview.getLineItems().stream()
                .map(DistributionPreviewDto.DistributionLineItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal delta = distributableAmount.subtract(expectedTotal);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        List<DistributionPreviewDto.DistributionLineItem> lines = preview.getLineItems();
        if (lines.isEmpty()) {
            return;
        }

        int index = 0;
        while (delta.compareTo(BigDecimal.ZERO) != 0) {
            DistributionPreviewDto.DistributionLineItem line = lines.get(index % lines.size());
            if (delta.signum() > 0) {
                line.setTotal(line.getTotal().add(ONE_CENT));
                if (line.getProfit().compareTo(BigDecimal.ZERO) > 0 || line.getCapitalRecovery().compareTo(BigDecimal.ZERO) == 0) {
                    line.setProfit(line.getProfit().add(ONE_CENT));
                } else {
                    line.setCapitalRecovery(line.getCapitalRecovery().add(ONE_CENT));
                }
                delta = delta.subtract(ONE_CENT);
            } else {
                line.setTotal(line.getTotal().subtract(ONE_CENT));
                if (line.getCapitalRecovery().compareTo(BigDecimal.ZERO) > 0) {
                    line.setCapitalRecovery(line.getCapitalRecovery().subtract(ONE_CENT));
                } else {
                    line.setProfit(line.getProfit().subtract(ONE_CENT));
                }
                delta = delta.add(ONE_CENT);
            }
            index++;
        }
    }

    private static class RatioEntry {
        private final Investment investment;
        private final BigDecimal exact;
        private final BigDecimal whole;
        private BigDecimal remainder;

        private RatioEntry(Investment investment, BigDecimal exact, BigDecimal whole, BigDecimal remainder) {
            this.investment = investment;
            this.exact = exact;
            this.whole = whole;
            this.remainder = remainder;
        }
    }

    private static class MapWithRounds {
        private final java.util.Map<Investment, BigDecimal> values = new java.util.HashMap<>();

        public void put(Investment investment, BigDecimal value) {
            if (investment != null) {
                values.put(investment, value == null ? BigDecimal.ZERO : value);
            }
        }

        public BigDecimal get(Investment investment) {
            if (investment == null) {
                return BigDecimal.ZERO;
            }
            return values.getOrDefault(investment, BigDecimal.ZERO);
        }

        public java.util.Collection<BigDecimal> values() {
            return values.values();
        }

        public BigDecimal getAmountFor(Investment investment) {
            if (investment == null) {
                return BigDecimal.ZERO;
            }
            return values.getOrDefault(investment, BigDecimal.ZERO);
        }
    }
}
