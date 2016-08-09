package co.uk.zerod.domain;

import mtymes.javafixes.object.Microtype;

public class TableName extends Microtype<String> {

    private TableName(String value) {
        super(value);
    }

    public static TableName tableName(String value) {
        return new TableName(value);
    }
}
