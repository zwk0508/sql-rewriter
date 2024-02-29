package com.zwk.sql;

public interface TableAction {
    SqlCondition meetTable(String tableName, String alias,JoinType joinType);
}
