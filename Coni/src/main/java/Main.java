import input.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    public static Connector connector1;
    public static Connector connector2;
    public static String target;
    public static String host = "localhost";
    public static String port = "3366";
    public static String user = "root";
    public static String password = "123456";
    public static String database = "test";
    public static long time = 60 * 60 * 1000L;
    public static String oracle;
    public static int maxTestUnit = 1;
    public static int minUnitFunc = 20;
    public static int maxUnitFunc = 50;
    public static int maxSameStateFunc = 3;
    public static final String out = "out/";
    public static final String blacklist = "src/main/resources/blacklist/";
    public static final String jdbc = "src/main/resources/jdbc.txt";
    public static final Class<?>[] targetClass = {DriverManager.class, Connection.class, Statement.class, PreparedStatement.class, ResultSet.class};
    public static AtomicLong sharedThreadNum = new AtomicLong(0);
    public static AtomicLong sharedTestCaseNum = new AtomicLong(0);
    public static AtomicLong sharedDiffNum = new AtomicLong(0);
    public static AtomicLong sharedBugNum = new AtomicLong(0);

    public static AtomicLong sharedInterfaceNum = new AtomicLong(0);
    public static AtomicLong sharedStateTransNum = new AtomicLong(0);
    public static AtomicLong sharedCorrectNum = new AtomicLong(0);
    private static final Logger logger = LogManager.getLogger("Main");
    
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.err.println(String.format("Program needs one arg: [config.properties path], but found: %s", args.length));
            return;
        }
        if (!initConfig(args[0]) || !initSpecification() || !initDir()) {
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            logger.fatal(String.format("Completed test cases: %s, Different test cases: %s, Potential Bugs: %s", sharedTestCaseNum.get(), sharedDiffNum.get(), sharedBugNum.get()));
        }, 0, 10, TimeUnit.SECONDS);

        long curTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < curTime + time) {
            long seed = System.currentTimeMillis();
            long currentEpoch = sharedThreadNum.getAndIncrement();
            TestCase cur = TestCaseGenerator.generateTestCase(new Random(seed), String.valueOf(currentEpoch));

            Runnable task = () -> {
                Random r1 = new Random(seed);
                Random r2 = new Random(seed);
                TestCaseExecutor executor1 = new TestCaseExecutor(cur, connector1, r1);
                TestCaseExecutor executor2 = new TestCaseExecutor(cur, connector2, r2);
                if ("cg".equals(Main.oracle)) {
                    executor1.setConfig(true);
                    executor2.setConfig(false);
                }
                List<FeedbackResult> res1 = executeTestCase(currentEpoch, executor1);
                List<FeedbackResult> res2 = executeTestCase(currentEpoch, executor2);
                if (res1 != null || res2 != null) {
                    ResultMonitor rm = new ResultMonitor(String.valueOf(currentEpoch), seed, res1, res2, r1, r2);
                    if (!rm.checkResult()) {
                        sharedDiffNum.incrementAndGet();
                    }
                    sharedTestCaseNum.incrementAndGet();
                }
            };
            executorService.submit(task);
            Thread.sleep(500);
        }

        executorService.shutdown();
        try {
            System.out.println("Wait for thread to terminate...");
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("Time out, terminate all threads...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted, terminate all threads...");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.fatal(String.format("Completed test cases: %s, Different test cases: %s, Potential Bugs: %s", sharedTestCaseNum.get(), sharedDiffNum.get(), sharedBugNum.get()));

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static List<FeedbackResult> executeTestCase(long epoch, TestCaseExecutor executor) {
        List<FeedbackResult> res = null;
        ExecutorService clearExec = Executors.newSingleThreadExecutor();
        Future<Boolean> future = clearExec.submit(() -> clearDatabase(String.valueOf(epoch)));
        boolean ready = false;
        try {
            ready = future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.println(String.format("Timeout when clear database %s", epoch));
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(String.format("Unexpected error when clear database %s, %s", epoch, e));
        } finally {
            clearExec.shutdownNow();
        }
        if (ready) {
            try {
                res = executor.executeTestCase();
            } catch (SQLException | IllegalAccessException e) {
                System.err.println(String.format("Unexpected error when execute test case%s using connector %s, %s", epoch, connector1, e));
            }
        } else {
            return res;
        }
        return res;
    }

    private static boolean clearDatabase(String id) {
        String url = String.format("jdbc:%s://%s:%s/%s?user=%s&password=%s",
                connector1, host, port, target, user, password);
        try {
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + database + id);
            stmt.executeUpdate("CREATE DATABASE " + database + id);
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.err.println(String.format("Unexpected error when clear database %s, %s", id, e));
            return false;
        }
        return true;
    }

    private static boolean initDir() {
        File currentDir = new File(System.getProperty("user.dir"));
        File outDir = new File(currentDir, "out");

        if (outDir.exists() && outDir.isDirectory()) {
            File[] files = outDir.listFiles();
            if (files != null) {
                clearDirectory(outDir);
            }
            System.out.println("Make dir out...");
            return true;
        } else {
            if (outDir.mkdir()) {
                System.out.println("Make dir out...");
                return true;
            } else {
                System.err.println("Make dir out failed...");
                return false;
            }
        }
    }

    private static void clearDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectory(file);
                }
                file.delete();
            }
        }
    }

    private static boolean initSpecification() {
        System.out.println("Load JDBC specification...");
        if (!TestCaseGenerator.initFunc()) {
            System.err.println("Load JDBC specification failed");
        }
        if (!ResultMonitor.initBlackList(target)) {
            System.err.println("Load blacklist failed");
        }
        return true;
    }

    private static boolean initConfig(String arg) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(arg));
        } catch (IOException e) {
            System.err.println(String.format("Properties file not found, path: %s", arg));
            return false;
        }
        target = prop.getProperty("target");
        if (!"mysql".equals(target) && !"postgres".equals(target)) {
            System.err.println(String.format("Unsupported database: %s, please use [mysql] or [postgresql]", target));
            return false;
        }
        host = prop.getProperty("host", host);
        port = prop.getProperty("port", port);
        user = prop.getProperty("user", user);
        password = prop.getProperty("password", password);
        maxTestUnit = Integer.parseInt(prop.getProperty("max_unit", String.valueOf(maxTestUnit)));
        minUnitFunc = Integer.parseInt(prop.getProperty("min_func_in_unit", String.valueOf(minUnitFunc)));
        maxUnitFunc = Integer.parseInt(prop.getProperty("max_func_in_unit", String.valueOf(maxUnitFunc)));
        oracle = prop.getProperty("oracle");
        if ("cp".equals(oracle)) {
            String con1 = prop.getProperty("connector1");
            String con2 = prop.getProperty("connector2");
            connector1 = initConnector(con1);
            connector2 = initConnector(con2);
            if (connector1 == null || connector2 == null) {
                System.err.println(String.format("Please check the config, should have [connector1] and [connector2]", connector1, connector2));
                return false;
            }
            else if (connector1.equals(connector2)) {
                System.err.println("[connector1] and [connector2] should be different");
                return false;
            }
        } else {
            System.err.println(String.format("Unsupported test oracle: %s, please use [cp]", oracle));
            return false;
        }
        return true;
    }

    private static Connector initConnector(String connector) {
        switch (connector) {
            case "mysql":
                return Connector.MYSQL;
            case "mariadb":
                return Connector.MARIADB;
            case "awsmysql":
                return Connector.AWSMYSQL;
            case "awspostgres":
                return Connector.AWSWRAPPER;
            case "postgresql":
            case "postgres":
                return Connector.POSTGRESQL;
            case "ngpgsql":
            case "ngpostgres":
            case "ng":
                return Connector.NGPGSQL;
            default:
                return null;
        }
    }

    public enum Connector {
        MARIADB("mariadb"),
        MYSQL("mysql"),
        AWSMYSQL("mysql:aws"),
        POSTGRESQL("postgresql"),
        NGPGSQL("pgsql"),
        AWSWRAPPER("aws-wrapper:postgresql"),;

        private final String protocal;

        Connector(String protocal) {
            this.protocal = protocal;
        }

        @Override
        public String toString() {
            return protocal;
        }
    }
}
