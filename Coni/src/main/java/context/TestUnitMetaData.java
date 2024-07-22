package context;

import context.schema.Column;
import context.schema.Table;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUnitMetaData {
    private String id;
    private String db;
    private Connection con;
    private String tableName;
    private Table table;
    public int curPrepareType;
    public int curPrepareParamCnt;
    public String curPrepareSql;
    public Table curPrepareTable;
    public Column curPrepareColumn;
    public boolean configSetting = false;
    public Object config;
    public Savepoint savepoint;

    public Column getRandomTableColumn(Random r) {
        if (!this.hasTable()) {
            return null;
        }
        return this.table.getRandomColumn(r);
    }

    public boolean isAvailable() {
        return this.con != null && this.db != null;
    }

    public boolean hasTable() {
        return this.table != null;
    }

    public void initTable() {
        try {
            DatabaseMetaData metaData = con.getMetaData();
            ResultSet columns = metaData.getColumns(db, null, tableName, null);
            List<Column> tmpCols = new ArrayList<>();
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                Column col = new Column(columnName, columnType);
                tmpCols.add(col);
            }
            if (!tmpCols.isEmpty()) {
                this.table = new Table(tableName, tmpCols);
            }
            columns.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void init(Connection con) {
        this.con = con;
        this.db = "test" + this.id.split("_")[0];
    }

    public TestUnitMetaData(String id) {
        this.id = id;
        this.tableName = "table" + this.id;
    }

    public Table getTable() {
        return table;
    }

    public String getTableName() {
        return tableName;
    }
}
