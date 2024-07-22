package generator.sql;

import context.TestUnitMetaData;
import java.util.Random;

public class MySQLGenerator extends SQLGenerator {
    static String[] supportedValueType = {"INT", "TINYINT", "BIGINT", "SMALLINT",
            "FLOAT", "DOUBLE", "DECIMAL",
            "CHAR", "VARCHAR(5)", "TEXT(5)", "VARCHAR(100)", "TEXT(100)", "BOOL"};
    static String[] supportedKeyType = {"INT", "TINYINT", "BIGINT", "SMALLINT",
            "FLOAT", "DOUBLE", "DECIMAL",
            "CHAR", "VARCHAR(5)", "VARCHAR(100)", "BOOL"};
    static String[] supportedExpr = {"+", "-", "*", "/"};
    static String[] supportedOp = {"=", ">", "<", ">=", "<=", "<>", "!="};

    @Override
    public Object generateValueByType(String colType) {
        switch (colType.toUpperCase()) {
            case "DOUBLE":
            case "DECIMAL":
            case "REAL":
                return r.nextDouble();
            case "FLOAT":
                return r.nextFloat();
            case "INT":
            case "INTEGER":
            case "TINYINT":
            case "SMALLINT":
                return r.nextInt();
            case "BIGINT":
                return r.nextLong();
            case "BOOLEAN":
            case "BOOL":
            case "BIT":
                return r.nextBoolean();
            case "VARCHAR":
            case "TEXT":
            case "CHAR":
            default:
                String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*()!.,;";
                StringBuilder sb = new StringBuilder("'");
                int len = r.nextInt(50);
                for (int i = 0; i < len; i++) {
                    sb.append(characters.charAt(r.nextInt(characters.length())));
                }
                sb.append("'");
                return sb.toString();
        }
    }

    public MySQLGenerator(Random r, String id, TestUnitMetaData testUnitMetaData) {
        super(r, id, testUnitMetaData, supportedValueType, supportedKeyType, supportedExpr, supportedOp);
    }
}
