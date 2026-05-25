package com.ticketpurchasingsystem.project.domain.User;

public enum UserTableFields {
    ID("id"),
    NAME("name"),
    EMAIL("email"),
    PASSWORD("password"),
    STATE("state"),
    SESSION_TOKEN("session_token_str"),
    LOGGED_IN("logged_in"),
    USER_GROUP_DISCOUNT("user_group_discount");

    private final String fieldName;

    UserTableFields(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
