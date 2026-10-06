package com.malcolm.expensesplitter.command;

public interface Command {
    void execute();
    void undo();
}
