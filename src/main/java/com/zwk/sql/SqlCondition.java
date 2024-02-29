package com.zwk.sql;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/2/28 16:34
 */

public class SqlCondition {
    private String condition;
    private SqlLocation location = SqlLocation.WHERE;

    public static final SqlCondition NONE = new SqlCondition();

    public SqlCondition() {
    }

    public SqlCondition(String condition) {
        this.condition = condition;
    }

    public SqlCondition(String condition, SqlLocation location) {
        this.condition = condition;
        this.location = location;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public SqlLocation getLocation() {
        return location;
    }

    public void setLocation(SqlLocation location) {
        this.location = location;
    }
}
