package generator.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

public abstract class ConfigGenerator {
    protected Random r;

    public Object generateRandomResultConfig(String paraName) {
        if ("resultSetType".equals(paraName)) {
            return selectRandomValue(new int[]{ResultSet.TYPE_FORWARD_ONLY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE});
        } else if ("resultSetConcurrency".equals(paraName)) {
            return selectRandomValue(new int[]{ResultSet.CONCUR_READ_ONLY, ResultSet.CONCUR_UPDATABLE});
        } else if ("resultSetHoldability".equals(paraName)) {
            return selectRandomValue(new int[]{ResultSet.HOLD_CURSORS_OVER_COMMIT, ResultSet.CLOSE_CURSORS_AT_COMMIT});
        }
        return null;
    }

    public Object generateRandomTransactionConfig() {
        return selectRandomValue(new int[]{Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_READ_UNCOMMITTED,
                Connection.TRANSACTION_REPEATABLE_READ, Connection.TRANSACTION_SERIALIZABLE});
    }

    public Object generateRandomGenerateKeyConfig() {
        return selectRandomValue(new int[]{Statement.NO_GENERATED_KEYS, Statement.RETURN_GENERATED_KEYS});
    }

    public Object generateRandomFetchConfig() {
        return selectRandomValue(new int[]{ResultSet.FETCH_FORWARD, ResultSet.FETCH_REVERSE, ResultSet.FETCH_UNKNOWN});
    }

    public Object generateSimpleDataConfig() {
        if (r.nextInt(5) < 2) {
            return selectRandomValue(new int[]{-1, 0, Integer.MAX_VALUE, Integer.MIN_VALUE});
        } else {
            return r.nextInt(0, Integer.MAX_VALUE);
        }
    }

    public Object generateMoreResultsConfig() {
        return selectRandomValue(new int[]{Statement.CLOSE_CURRENT_RESULT, Statement.KEEP_CURRENT_RESULT, Statement.CLOSE_ALL_RESULTS});
    }

    public abstract Object generateRandomUrlConfig();

    public abstract Object generateRandomBoolUrlConfig();

    protected int selectRandomValue(int[] values) {
        int randomIndex = r.nextInt(values.length);
        return values[randomIndex];
    }

    public ConfigGenerator(Random r) {
        this.r = r;
    }
}
