package com.datalens.model;

public enum ColumnType {
    NUMBER("number"),
    DATE("date"),
    TEXT("text");

    private final String displayName;

    ColumnType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
