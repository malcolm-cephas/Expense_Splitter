package com.malcolm.expensesplitter.command;

import com.malcolm.expensesplitter.models.Expense;
import com.malcolm.expensesplitter.repositories.ExpenseRepository;

public class AddExpenseCommand implements Command {

    private final ExpenseRepository expenseRepository;
    private final Expense expense;

    public AddExpenseCommand(ExpenseRepository expenseRepository, Expense expense) {
        this.expenseRepository = expenseRepository;
        this.expense = expense;
    }

    @Override
    public void execute() {
        // The expense is usually saved initially by the service.
        // If we redo, we save it again.
        if (!expenseRepository.existsById(expense.getId())) {
            expenseRepository.save(expense);
        }
    }

    @Override
    public void undo() {
        expenseRepository.delete(expense);
    }
}
