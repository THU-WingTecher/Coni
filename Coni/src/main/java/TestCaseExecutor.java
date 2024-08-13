import input.ContextualFunc;
import input.TestCase;
import input.TestUnit;
import context.TestUnitMetaData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class TestCaseExecutor {
    private TestCase tc;
    private final List<TestUnitExecutor> availableExecutor;
    private final Random r;

    public List<FeedbackResult> executeTestCase() throws IllegalAccessException, SQLException {
        List<FeedbackResult> res = new ArrayList<>();
        while (existAvailableTestUnitExecutor()) {
            TestUnitExecutor randomUnitExecutor = getRandomAvailableTestUnitExecutor();
            FeedbackResult r = randomUnitExecutor.executeNextFunc();
            res.add(r);
            if (!randomUnitExecutor.isAvailable()) {
                availableExecutor.remove(randomUnitExecutor);
                // randomUnitExecutor.closeThread();
            }
        }
        return res;
    }

    private TestUnitExecutor getRandomAvailableTestUnitExecutor() {
        assert !this.availableExecutor.isEmpty();
        int pos = r.nextInt(this.availableExecutor.size());
        return this.availableExecutor.get(pos);
    }

    private boolean existAvailableTestUnitExecutor() {
        return !this.availableExecutor.isEmpty();
    }

    public TestCaseExecutor(TestCase tc, Main.Connector connector, Random r) {
        this.tc = tc;
        this.r = r;
        this.availableExecutor = new LinkedList<>();
        for (TestUnit tu : this.tc.getUnits()) {
            availableExecutor.add(new TestUnitExecutor(connector, r, tu));
        }
    }

    public void setConfig(boolean b) {
        for (TestUnitExecutor executor : this.availableExecutor) {
            executor.setConfig(b);
        }
    }
}

class TestUnitExecutor {
    private Main.Connector connector;
    private final TestUnit tu;
    private String id;
    private int currentPos;
    private ArgGenerator argGenerator;
    private TestUnitMetaData testUnitMetaData;
    private Connection con;
    private Statement stmt;
    private ResultSet resultSet;
    // private static ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Logger logger = LogManager.getLogger("TestUnitExecutor");

    public FeedbackResult executeNextFunc() throws SQLException {
        assert currentPos >= 0 && currentPos < tu.getFuncs().size();
        ContextualFunc func = tu.getFuncs().get(currentPos);
//         FeedbackResult res = execute(func);
//         currentPos++;
        FeedbackResult res = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<FeedbackResult> future = executor.submit(() -> execute(func));
        try {
            res = future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            res = new FeedbackResult(id, func, null, "Timeout");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            currentPos++;
            executor.shutdownNow();
        }
        return res;
    }

    private FeedbackResult execute(ContextualFunc func) throws SQLException {
        Method method = func.getMethod();
        String[] names = func.getParamName();
        String cls = method.getDeclaringClass().getSimpleName();
        Object originResult = null;
        Object[] args = new Object[0];
        // logger.info("before: " + cls + "." + method.getName() + " " + func.getPreviousStates());
        try {
            if ("DriverManager".equals(cls)) {
                args = argGenerator.generateRandomDriverArgs(method, names);
                originResult = method.invoke(null, args);
            }
            else if ("Connection".equals(cls)) {
                assert this.con != null;
                args = argGenerator.generateRandomConnArgs(method, names);
                originResult = method.invoke(this.con, args);
            } else if ("PreparedStatement".equals(cls)) {
                assert this.stmt != null;
                List<Object[]> preparedArgs = argGenerator.generateRandomPreparedStmtArgs(method, names);
                if (preparedArgs != null) {
                    args = new String[preparedArgs.size()];
                    for (int i = 0; i < preparedArgs.size(); i++) {
                        Object[] cur = preparedArgs.get(i);
                        originResult = method.invoke(this.stmt, cur);
                        args[i] = Arrays.toString(cur);
                    }
                }
                else {
                    originResult = method.invoke(this.stmt, (Object[]) null);
                }
            } else if ("Statement".equals(cls)) {
                assert this.stmt != null;
                args = argGenerator.generateRandomStmtArgs(method, names);
                originResult = method.invoke(this.stmt, args);
            } else if ("ResultSet".equals(cls)) {
                if (this.resultSet == null) {
                    // logger.info("{} failed", cls + "." + method.getName());
                    return new FeedbackResult(this.connector.toString() + ": unit" + this.id, func, args, "No ResultSet object");
                } else {
                    args = argGenerator.generateRandomResultArgs(method, names);
                    originResult = method.invoke(this.resultSet, args);
                }
            }
            // logger.info("{}, args: {}", cls + "." + method.getName(), Arrays.toString(args));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            return new FeedbackResult(this.connector.toString() + ": unit" + this.id, func, args, cause);
        } catch (Exception e) {
            // logger.error("{}, args: {}, e: {}", cls + "." + method.getName(), Arrays.toString(args), e);
            return new FeedbackResult(this.connector.toString() + ": unit" + this.id, func, args, e);
        }

        if (createConnectorObject(originResult)) {
            return new FeedbackResult(this.connector.toString() + ": unit" + this.id, func, args, originResult.getClass().getSimpleName(), true);
        } else {
            return new FeedbackResult(this.connector.toString() + ": unit" + this.id, func, args, originResult);
        }
    }

    private boolean createConnectorObject(Object originResult) throws SQLException {
        if (originResult instanceof ResultSet) {
            // logger.info("Create new ResultSet object");
            this.resultSet = (ResultSet) originResult;
            return true;
        } else if (originResult instanceof CallableStatement) {
            // logger.info("Create new CallableStatement object");
            this.stmt = (CallableStatement) originResult;
            return true;
        } else if (originResult instanceof PreparedStatement) {
            // logger.info("Create new PreparedStatement object");
            this.stmt = (PreparedStatement) originResult;
            return true;
        } else if (originResult instanceof Statement) {
            // logger.info("Create new Statement object");
            this.stmt = (Statement) originResult;
            return true;
        } else if (originResult instanceof Connection) {
            // logger.info("Create new Connection object");
            this.con = (Connection) originResult;
            this.testUnitMetaData.init(this.con);
            return true;
        } else if (originResult instanceof Savepoint) {
            // logger.info("Create new Savepoint object");
            this.testUnitMetaData.savepoint = (Savepoint) originResult;
            return true;
        }
        return false;
    }

    public boolean isAvailable() {
        return !tu.getFuncs().isEmpty() && currentPos < tu.getFuncs().size();
    }

    public TestUnitExecutor(Main.Connector connector, Random r, TestUnit tu) {
        this.connector = connector;
        this.tu = tu;
        this.id = tu.getId();
        this.testUnitMetaData = new TestUnitMetaData(this.id);
        this.argGenerator = new ArgGenerator(r, testUnitMetaData, connector, this.id);
    }

    public void setConfig(boolean b) {
        this.testUnitMetaData.configSetting = b;
        this.testUnitMetaData.config = this.argGenerator.generateRandomBoolUrlConfig();
    }
}
