package generator.config;

import java.util.Random;

public class MySQLConfigGenerator extends ConfigGenerator{
    @Override
    public Object generateRandomUrlConfig() {
        StringBuilder sb = new StringBuilder();
        for (UrlConfig config : UrlConfig.values()) {
            if (this.r.nextBoolean()) {
                sb.append(getSpecificConfig(r, config));
            }
        }
        return sb.toString();
    }

    @Override
    public Object generateRandomBoolUrlConfig() {
        int op = r.nextInt(3);
        if (op == 0) {
            return "&useServerPrepStmts=";
        } else if (op == 1) {
            return "&allowMultiQueries=";
        } else {
            return "&rewriteBatchedStatements=";
        }
    }

    private static String getSpecificConfig(Random r, UrlConfig config) {
        switch (config) {
            case USE_SERVER_PREP_STMTS: {
                return String.format("&%s=%s", "useServerPrepStmts", r.nextBoolean());
            }
            case ALLOW_MULTI_QUERIES: {
                return String.format("&%s=%s", "allowMultiQueries", true);
            }
            case REWRITE_BATCHED_STMT:{
                return String.format("&%s=%s", "rewriteBatchedStatements", true);
            }
            case DUMP_QUERIES_ON_EXCEPTION:{
                return String.format("&%s=%s", "dumpQueriesOnException", true);
            }
            case TINY_INT1_IS_BIT:{
                return String.format("&%s=%s", "tinyInt1isBit", false);
            }
            case YEAR_IS_DATE_TYPE:{
                return String.format("&%s=%s", "yearIsDateType", false);
            }
            case CREATE_DATABASE_IF_NOT_EXIST:{
                return String.format("&%s=%s", "createDatabaseIfNotExist", true);
            }
            case CACHE_CALLABLE_STMTS:{
                return String.format("&%s=%s", "cacheCallableStmts", r.nextBoolean());
            }
            case USE_BULK_STMTS:{
                return String.format("&%s=%s", "useBulkStmts", r.nextBoolean());
            }
            default:
                return "";
        }
    }

    enum UrlConfig {
        USE_SERVER_PREP_STMTS,
        ALLOW_MULTI_QUERIES,
        REWRITE_BATCHED_STMT,
        DUMP_QUERIES_ON_EXCEPTION,
        TINY_INT1_IS_BIT,
        YEAR_IS_DATE_TYPE,
        CREATE_DATABASE_IF_NOT_EXIST,
        CACHE_CALLABLE_STMTS,
        USE_BULK_STMTS,
    }

    public MySQLConfigGenerator(Random r) {
        super(r);
    }
}
