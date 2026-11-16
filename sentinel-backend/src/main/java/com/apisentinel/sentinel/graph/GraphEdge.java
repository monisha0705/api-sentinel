package com.apisentinel.sentinel.graph;

public final class GraphEdge {

    private final RelationshipType relationship;
    private final String description;

    public GraphEdge(RelationshipType relationship, String description) {
        this.relationship = relationship;
        this.description = description;
    }

    public RelationshipType relationship() {
        return relationship;
    }

    public String description() {
        return description;
    }
}


