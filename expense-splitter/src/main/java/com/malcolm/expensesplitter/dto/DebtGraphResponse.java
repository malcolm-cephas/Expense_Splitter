package com.malcolm.expensesplitter.dto;

import java.math.BigDecimal;
import java.util.List;

public class DebtGraphResponse {
    private List<Node> nodes;
    private List<Edge> edges;

    public DebtGraphResponse(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public static class Node {
        private String id;

        public Node(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class Edge {
        private String from;
        private String to;
        private BigDecimal amount;

        public Edge(String from, String to, BigDecimal amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
