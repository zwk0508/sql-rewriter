package com.zwk.sql;


import com.zwk.sql.parser.SqlBaseBaseVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import static com.zwk.sql.parser.SqlBaseParser.*;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/2/27 16:45
 */

public class SqlVisitor extends SqlBaseBaseVisitor<Void> {

    private TokenStreamRewriter rewriter;

    private TableAction tableAction;

    private Stack<QuerySpecificationContext> stack = new Stack<>();

    private Map<ParseTree, String> aliasMap = new HashMap<>();
    private Map<ParseTree, JoinType> joinTypeMap = new HashMap<>();
    private Map<ParseTree, JoinCriteriaContext> joinedCriteriaMap = new HashMap<>();
    private Map<ParseTree, Boolean> whereExists = new HashMap<>();


    public SqlVisitor(CommonTokenStream tokenStream, TableAction tableAction) {
        this.rewriter = new TokenStreamRewriter(tokenStream);
        this.tableAction = tableAction;
    }

    public String toSql() {
        return rewriter.getText();
    }


    @Override
    public Void visitQuerySpecification(QuerySpecificationContext ctx) {
        stack.push(ctx);
        List<ParseTree> children = ctx.children;
        if (children != null) {
            for (ParseTree child : children) {
                child.accept(this);
            }
        }
        stack.pop();
        return null;
    }

    @Override
    public Void visitAliasedRelation(AliasedRelationContext ctx) {
        IdentifierContext identifier = ctx.identifier();

        RelationPrimaryContext relationPrimaryContext = ctx.relationPrimary();

        if (identifier != null) {
            String alias = getIdentifier(identifier);
            aliasMap.put(relationPrimaryContext, alias);
        }
        visit(relationPrimaryContext);

        return null;
    }

    @Override
    public Void visitJoinRelation(JoinRelationContext ctx) {
        visit(ctx.left);
        JoinCriteriaContext joinedCriteria = ctx.joinCriteria();
        RelationContext rightRelation = ctx.rightRelation;
        if (joinedCriteria != null) {
            JoinTypeContext joinType = ctx.joinType();
            if (rightRelation instanceof RelationDefaultContext) {
                SampledRelationContext samplededRelation = ((RelationDefaultContext) rightRelation).sampledRelation();
                AliasedRelationContext aliasededRelation = samplededRelation.aliasedRelation();
                RelationPrimaryContext relationPrimary = aliasededRelation.relationPrimary();
                if (relationPrimary instanceof TableNameContext) {
                    JoinType type = JoinType.NONE;
                    if (joinType == null) {
                        type = JoinType.INNER;
                    } else if (joinType.LEFT() != null) {
                        type = JoinType.LEFT;
                    } else if (joinType.RIGHT() != null) {
                        type = JoinType.RIGHT;
                    } else if (joinType.FULL() != null) {
                        type = JoinType.FULL;
                    } else {
                        type = JoinType.INNER;
                    }
                    joinTypeMap.put(relationPrimary, type);

                    joinedCriteriaMap.put(relationPrimary, joinedCriteria);
                }
            }

        }

        if (ctx.right != null) {
            visit(ctx.right);
        }
        if (rightRelation != null) {
            visit(rightRelation);
        }

        return null;
    }

    @Override
    public Void visitTableName(TableNameContext ctx) {

        QualifiedNameContext qualifiedName = ctx.qualifiedName();

        String tableName = qualifiedName.identifier().stream().map(this::getIdentifier).collect(Collectors.joining("."));

        String alias = aliasMap.get(ctx);
        JoinType joinType = joinTypeMap.get(ctx);
        if (joinType == null) {
            joinType = JoinType.NONE;
        }

        SqlCondition sqlCondition = tableAction.meetTable(tableName, alias, joinType);


        if (sqlCondition != SqlCondition.NONE) {
            SqlLocation location = sqlCondition.getLocation();
            if (location == SqlLocation.WHERE) {
                QuerySpecificationContext context = stack.peek();
                BooleanExpressionContext where = context.where;
                Token token;
                String sql = sqlCondition.getCondition();
                Boolean b = whereExists.get(context);
                if (where != null) {
                    token = where.stop;
                    sql = " and " + sql;
                    whereExists.put(context, true);
                } else if (b == null) {
                    token = context.getStop();
                    sql = " where " + sql;
                    whereExists.put(context, true);
                } else {
                    token = context.getStop();
                    sql = " and " + sql;
                }

                rewriter.insertAfter(token, sql);
            } else {
                JoinCriteriaContext joinCriteriaContext = joinedCriteriaMap.get(ctx);
                TerminalNode terminalNode = joinCriteriaContext.ON();
                if (terminalNode == null) {
                    throw new IllegalArgumentException("only support on join criteria");
                }
                rewriter.insertAfter(terminalNode.getSymbol(), " " + sqlCondition.getCondition() + " and ");
            }
        }

        return null;
    }


    private String getIdentifier(IdentifierContext context) {
        String text = context.getText();
        if (text.startsWith("`") || text.startsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }
}
