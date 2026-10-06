package com.malcolm.expensesplitter.command;

import org.springframework.stereotype.Service;
import java.util.Stack;

@Service
public class CommandManager {
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>(); // Optional for later

    public void executeCommand(Command command) {
        command.execute();
        pushCommand(command);
    }

    public void pushCommand(Command command) {
        undoStack.push(command);
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }
}
