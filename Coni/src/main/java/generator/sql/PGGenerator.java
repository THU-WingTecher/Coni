package generator.sql;

import context.TestUnitMetaData;
import java.util.Random;

public class PGGenerator extends SQLGenerator {
    static String[] supportedValueType = {"INT", "MONEY", "BIGINT", "SMALLINT",
            "BOOLEAN", "BIT",
            "FLOAT", "REAL", "DECIMAL",
            "VARCHAR(5)", "VARCHAR(100)", "TEXT"};
    static String[] supportedKeyType = {"INT", "MONEY", "BIGINT", "SMALLINT",
            "BOOLEAN", "BIT",
            "FLOAT", "REAL", "DECIMAL",
            "VARCHAR(5)", "VARCHAR(100)"};
    static String[] supportedExpr = {"+", "-", "*", "/"};
    static String[] supportedOp = {"=", ">", "<", ">=", "<=", "<>", "!="};

    public Object generateValueByType(String colType) {
        switch (colType.toUpperCase()) {
            case "DOUBLE":
            case "DECIMAL":
            case "REAL":
                return r.nextDouble();
            case "FLOAT":
            case "FLOAT4":
                return r.nextFloat();
            case "INT":
            case "INTEGER":
            case "TINYINT":
            case "SMALLINT":
            case "INT8":
                return r.nextInt();
            case "BIGINT":
            case "MONEY":
                return r.nextLong();
            case "BOOLEAN":
            case "BOOL":
                return r.nextBoolean();
            case "BIT":
                return r.nextInt(100) + "::bit";
            case "VARCHAR":
            case "TEXT":
            case "CHAR":
                String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&()!.;";
                StringBuilder sb = new StringBuilder("'");
                int len = r.nextInt(50);
                for (int i = 0; i < len; i++) {
                    sb.append(characters.charAt(r.nextInt(characters.length())));
                }
                sb.append("'");
                return sb.toString();
            default:
                return r.nextInt();
        }
    }

    public PGGenerator(Random r, String id, TestUnitMetaData testUnitMetaData) {
        super(r, id, testUnitMetaData, supportedValueType, supportedKeyType, supportedExpr, supportedOp);
    }
}
