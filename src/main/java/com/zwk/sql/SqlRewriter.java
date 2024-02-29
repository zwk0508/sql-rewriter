package com.zwk.sql;


import com.zwk.sql.parser.CaseInsensitiveStream;
import com.zwk.sql.parser.SqlBaseLexer;
import com.zwk.sql.parser.SqlBaseParser;
import org.antlr.v4.runtime.*;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/2/28 19:30
 */

public class SqlRewriter {

    private TableAction tableAction;

    public SqlRewriter(TableAction tableAction) {
        this.tableAction = tableAction;
    }

    public String rewrite(String sql) {
        SqlBaseLexer lexer = new SqlBaseLexer(new CaseInsensitiveStream(CharStreams.fromString(sql)));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        SqlBaseParser parser = new SqlBaseParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(new MyErrorListener());
        SqlBaseParser.SingleStatementContext statement = parser.singleStatement();
        SqlVisitor visitor = new SqlVisitor(tokenStream, tableAction);
        statement.accept(visitor);
        return visitor.toSql();
    }

    static class MyErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            String errorMsg = "行: " + line + ",列: " + charPositionInLine + ",错误: " + msg;
            throw new RuntimeException(errorMsg);
        }
    }
}
