package generator.sql;

import context.TestUnitMetaData;
import context.schema.Column;
import context.schema.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class SQLGenerator {
    protected int SELECT = 1;
    protected int INSERT = 2;
    protected int UPDATE = 3;
    protected int DELETE = 4;
    protected Random r;
    protected String id;
    protected TestUnitMetaData testUnitMetaData;
    protected String[] supportedValueType;
    protected String[] supportedKeyType;
    protected String[] supportedExpr;
    protected String[] supportedOp;

    public abstract Object generateValueByType(String colType);

    public Object generateRandomPreparedSql() {
        assert this.isAvailable();
        if (!this.testUnitMetaData.hasTable()) {
            this.testUnitMetaData.initTable();
            if (!this.testUnitMetaData.hasTable()) {
                return generateConstantSelect();
            }
        }

        int sql = getRandomValue(new int[]{SELECT, INSERT});
        // store the needed information for the param generation for prepared statement
        this.testUnitMetaData.curPrepareTable = this.testUnitMetaData.getTable();
        this.testUnitMetaData.curPrepareSql = "";
        this.testUnitMetaData.curPrepareType = sql;
        this.testUnitMetaData.curPrepareParamCnt = 0;
        if (sql == SELECT) {
            return generatePreparedSelect();
        } else {
            return generatePreparedInsert();
        }
    }

    protected String generatePreparedSelect() {
        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        String col = table.getRandomColumn(this.r).toString();
        StringBuffer tmp = new StringBuffer("SELECT ");
        tmp.append(col).append(" FROM ").append(name);
        tmp.append(" WHERE ");
        String op = getRandomValue(supportedOp);
        tmp.append(col).append(" ").append(op).append(" ?");
        this.testUnitMetaData.curPrepareParamCnt++;
        this.testUnitMetaData.curPrepareSql = tmp.toString();
        return this.testUnitMetaData.curPrepareSql;
    }

    protected String generatePreparedInsert() {
        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        StringBuffer tmp = new StringBuffer("INSERT INTO ");
        tmp.append(name).append(" VALUES(?");
        this.testUnitMetaData.curPrepareParamCnt++;
        for (int i = 0; i < table.getCols().size() - 1; i++) {
            this.testUnitMetaData.curPrepareParamCnt++;
            tmp.append(", ?");
        }
        tmp.append(");");
        this.testUnitMetaData.curPrepareSql = tmp.toString();
        return this.testUnitMetaData.curPrepareSql;
    }

    public List<Object[]> generateRandomPreparedSqlValue() {
        List<Object[]> res = new ArrayList<>();
        // do not have a prepared statement
        if (this.testUnitMetaData.curPrepareParamCnt == 0 && this.testUnitMetaData.curPrepareTable == null) {
            res.add(new Object[]{1, "Default value"});
            return res;
        }
        for (int i = 1; i <= testUnitMetaData.curPrepareParamCnt; i++) {
            Object[] args = new Object[2];
            args[0] = i;
            if (this.testUnitMetaData.curPrepareType == SELECT) {
                if (i == 1) {
                    this.testUnitMetaData.curPrepareColumn = this.testUnitMetaData.curPrepareTable.getRandomColumn(this.r);
                    args[1] = this.generateValueByType(this.testUnitMetaData.curPrepareColumn.getType());
                } else {
                    throw new IllegalArgumentException("Unsupported select prepared statement!");
                }
            } else if (this.testUnitMetaData.curPrepareType == INSERT) {
                Column cur = this.testUnitMetaData.curPrepareTable.getCols().get(i - 1);
                args[1] = this.generateValueByType(cur.getType());
            }
            res.add(args);
        }
        return res;
    }

    public Object generateRandomSql(){
        assert this.isAvailable();
        if (r.nextBoolean() || !this.testUnitMetaData.hasTable()) {
            return generateRandomUpdateSql();
        } else {
            return generateRandomSelectSql();
        }
    }

    public Object generateRandomSelectSql(){
        assert this.isAvailable();
        // if no table, generate a constant select
        if (!this.testUnitMetaData.hasTable()) {
            this.testUnitMetaData.initTable();
            if (!this.testUnitMetaData.hasTable()) {
                return generateConstantSelect();
            }
        }

        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        List<Column> cols = table.getCols();
        StringBuffer tmp = new StringBuffer("SELECT ");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                tmp.append(", ");
            }
            String colName = cols.get(i).getName();
            tmp.append(colName);
        }
        tmp.append(" FROM ").append(name);
        if (r.nextBoolean()) {
            tmp.append(" WHERE ");
            tmp.append(generateWhereCondition());
        }
        tmp.append(";");
        return tmp.toString();
    }

    public Object generateRandomUpdateSql(){
        assert this.isAvailable();
        if (!this.testUnitMetaData.hasTable()) {
            this.testUnitMetaData.initTable();
            if (!this.testUnitMetaData.hasTable()) {
                return generateCreateSql();
            }
        }
        int sql = getRandomValue(new int[]{INSERT, UPDATE, DELETE});
        if (sql == INSERT) {
            return generateInsertSql();
        } else if (sql == UPDATE) {
            return generateUpdateSql();
        } else {
            return generateDeleteSql();
        }
    }

    protected Object generateConstantSelect() {
        StringBuffer tmp = new StringBuffer("SELECT ");
        String expr = generateValueExprByType("INT");
        tmp.append(expr);
        tmp.append(";");
        return tmp.toString();
    }

    protected Object generateCreateSql() {
        StringBuffer tmp = new StringBuffer("CREATE TABLE ");
        tmp.append(testUnitMetaData.getTableName());
        String idType = getRandomValue(supportedKeyType);
        tmp.append("(id ").append(idType).append(" PRIMARY KEY,");
        String valueType = getRandomValue(supportedValueType);
        tmp.append("value ").append(valueType).append(");");
        return tmp.toString();
    }

    protected Object generateInsertSql() {
        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        List<Column> cols = table.getCols();
        StringBuffer tmp = new StringBuffer("INSERT INTO " + name + " VALUES(");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                tmp.append(", ");
            }
            String colType = cols.get(i).getType();
            tmp.append(generateValueByType(colType));
        }
        tmp.append(")");
        return tmp.toString();
    }

    protected Object generateUpdateSql() {
        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        // only update value
        Column valueCol = table.getValueCol();
        StringBuffer tmp = new StringBuffer("UPDATE " + name + " SET value = ");
        tmp.append(generateValueByType(valueCol.getType()));
        tmp.append(" WHERE ");
        tmp.append(generateWhereCondition());
        tmp.append(";");
        return tmp.toString();
    }

    protected Object generateDeleteSql() {
        Table table = this.testUnitMetaData.getTable();
        String name = table.getName();
        StringBuffer tmp = new StringBuffer("DELETE FROM " + name);
        tmp.append(" WHERE ");
        tmp.append(generateWhereCondition());
        tmp.append(";");
        return tmp.toString();
    }

    protected String generateWhereCondition() {
        int num = this.r.nextInt(1, 3);
        StringBuffer tmp = new StringBuffer();
        for (int i = 0; i < num; i++) {
            if (i == 0) {
                tmp.append(generateBoolExpr());
            } else {
                if (r.nextBoolean()) {
                    tmp.append(" AND ");
                } else {
                    tmp.append(" OR ");
                }
                tmp.append(generateBoolExpr());
            }
        }
        return tmp.toString();
    }

    protected String generateBoolExpr() {
        String op = getRandomValue(supportedOp);
        Column col = this.testUnitMetaData.getRandomTableColumn(this.r);
        String left = col.toString();
        String right;
        if (r.nextBoolean()) {
            right = generateValueByType(col.getType()).toString();
        } else {
            right = generateValueExprByType(col.getType());
        }
        return String.format("(%s %s %s)", left, op, right);
    }

    protected String generateValueExprByType(String type) {
        String op = getRandomValue(supportedExpr);
        StringBuffer tmp = new StringBuffer();
        int valueNum = r.nextInt(1, 4);
        for (int i = 0; i < valueNum; i++) {
            Object value = generateValueByType(type);
            if (i == 0) {
                tmp.append(value);
            } else {
                tmp.append(op);
                tmp.append(value);
            }
        }
        return tmp.toString();
    }

    protected boolean isAvailable() {
        return this.testUnitMetaData.isAvailable();
    }

    protected int getRandomValue(int[] values) {
        int randomIndex = r.nextInt(values.length);
        return values[randomIndex];
    }

    protected String getRandomValue(String[] values) {
        int randomIndex = r.nextInt(values.length);
        return values[randomIndex];
    }

    public SQLGenerator(Random r, String id, TestUnitMetaData testUnitMetaData,
    String[] supportedValueType, String[] supportedKeyType, String[] supportedExpr, String[] supportedOp) {
        this.r = r;
        this.id = id;
        this.testUnitMetaData = testUnitMetaData;
        this.supportedValueType = supportedValueType;
        this.supportedKeyType = supportedKeyType;
        this.supportedExpr = supportedExpr;
        this.supportedOp = supportedOp;
    }
}
