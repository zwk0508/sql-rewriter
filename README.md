# sql-rewriter

### sql重写简介

```text
在sql中存在特定表时，在where或on后添加额外指定的条件语句
```

### com.zwk.sql.JoinType

sql表连接类型：inner left right full

### com.zwk.sql.SqlLocation

添加条件语句的位置 where还是on后面

### com.zwk.sql.SqlCondition

sql条件 condition: 拼接的语句 location: 拼接的位置

### com.zwk.sql.TableAction

meetTable方法有三个参数:

1. 表名 不带引号或反引号的单纯表名 sql中 `database`.`table_name` 会被转为 database.table_name
2. 别名 同表名处理方式
3. 连接类型 参见com.zwk.sql.JoinType

meetTable方法返回值: 参见com.zwk.sql.SqlCondition 返回com.zwk.sql.SqlCondition.NONE代表什么都不做

### 使用示例

```java
//添加简单为where条件
public static void test01() {
    String sql = "select * from user";
    TableAction ta = (tableName, alias, joinType) -> new SqlCondition("age = 18");
    SqlRewriter sqlRewriter = new SqlRewriter(ta);
    sql = sqlRewriter.rewrite(sql);
    System.out.println(sql);// --> select * from user where age = 18
}
```

```java
//添加where 和 on 条件
 public static void test02() {
    String sql = "select * from user u left join dep d on u.dep_id = d.id";
    TableAction ta = (tableName, alias, joinType) -> {

        if (tableName.equals("user")) {
            SqlLocation sqlLocation = SqlLocation.WHERE;
            if (joinType == JoinType.LEFT || joinType == JoinType.RIGHT) {
                sqlLocation = SqlLocation.ON;
            }
            String s = "age = 18";
            if (alias != null) {
                s = alias + "." + s;
            }
            return new SqlCondition(s, sqlLocation);
        }

        if (tableName.equals("dep")) {
            SqlLocation sqlLocation = SqlLocation.WHERE;
            if (joinType == JoinType.LEFT || joinType == JoinType.RIGHT) {
                sqlLocation = SqlLocation.ON;
            }
            String s = "dep_name = 'hr'";
            if (alias != null) {
                s = alias + "." + s;
            }
            return new SqlCondition(s, sqlLocation);
        }

        return SqlCondition.NONE;
    };
    SqlRewriter sqlRewriter = new SqlRewriter(ta);
    sql = sqlRewriter.rewrite(sql);
    System.out.println(sql);//--> select * from user u left join dep d on d.dep_name = 'hr' and  u.dep_id = d.id where u.age = 18
}
```

```java
//添加子查询 where 条件
public static void test03() {
    String sql = "select * from user where dep_id in(select dep_id from dep)";
    TableAction ta = (tableName, alias, joinType) ->
    {
        if (tableName.equals("dep")) {
            String s = "dep_name = 'hr'";
            if (alias != null) {
                s = alias + "." + s;
            }
            return new SqlCondition(s);
        }
        return SqlCondition.NONE;

    };
    SqlRewriter sqlRewriter = new SqlRewriter(ta);
    sql = sqlRewriter.rewrite(sql);
    System.out.println(sql);//-->select * from user where dep_id in(select dep_id from dep where dep_name = 'hr')
}
```



