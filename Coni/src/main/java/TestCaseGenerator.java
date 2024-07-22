import input.ContextualFunc;
import input.TestCase;
import input.TestUnit;
import state.ConnState;
import state.ConnectorState;
import state.ResultState;
import state.StmtState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class TestCaseGenerator {
    private static Map<ConnectorState, List<ContextualFunc>> sameStateFuncMap;
    private static Map<ConnectorState, List<ContextualFunc>> changedStateFuncMap;
    private static List<ContextualFunc> startFunc;
    private static List<ContextualFunc> endFunc;

    public static boolean initFunc() {
        sameStateFuncMap = new HashMap<>();
        changedStateFuncMap = new HashMap<>();
        startFunc = new ArrayList<>();
        endFunc = new ArrayList<>();
        FuncReader.init(sameStateFuncMap, changedStateFuncMap, startFunc, endFunc);
        return !sameStateFuncMap.isEmpty() && !changedStateFuncMap.isEmpty() && !startFunc.isEmpty() && !endFunc.isEmpty();
    }

    public static TestCase generateTestCase(Random r, String id) {
        List<TestUnit> units = new ArrayList<>();
        int unitCnt = r.nextInt(1, Main.maxTestUnit + 1);
        Set<String> method = new HashSet<>();
        for (int i = 0; i < unitCnt; i++) {
            TestUnit u = generateTestUnit(r, id + "_" + i, method);
            units.add(u);
        }
        Main.sharedInterfaceNum.addAndGet(method.size());
        return new TestCase(id, units);
    }

    private static TestUnit generateTestUnit(Random r, String id, Set<String> method) {
        List<ContextualFunc> funcs = new ArrayList<>();
        int funcCnt = r.nextInt(Main.minUnitFunc, Main.maxUnitFunc + 1);
        // start connection
        funcs.add(startFunc.get(0));
        ConnectorState s = startFunc.get(0).getNextState();
        int i = 0;
        int state = 0;
        while (i < funcCnt) {
            ContextualFunc func = selectChangedStateAvailableFunc(r, s);
            funcs.add(func);
            method.add(func.getName());
            s = func.getNextState();
            state += 1;
            List<ContextualFunc> sameStateFuncs = selectSameStateAvailableFunc(r, s);
            funcs.addAll(sameStateFuncs);
            for (ContextualFunc sameStateFunc : sameStateFuncs) {
                method.add(sameStateFunc.getName());
            }
            i = funcs.size();
        }
        // end connection
        for (ContextualFunc func : endFunc) {
            funcs.add(func);
        }
        Main.sharedStateTransNum.addAndGet(state);
        return new TestUnit(id, funcs);
    }

    private static List<ContextualFunc> selectSameStateAvailableFunc(Random r, ConnectorState s) {
        List<ContextualFunc> func = new ArrayList<>();
        List<ContextualFunc> curAvailableFunc = sameStateFuncMap.get(s);
        if (curAvailableFunc == null) {
            return func;
        }
        int funcNum = r.nextInt(Main.maxSameStateFunc);
        for (int i = 0; i < funcNum; i++) {
            int idx = r.nextInt(curAvailableFunc.size());
            func.add(curAvailableFunc.get(idx));
        }
        return func;
    }

    private static ContextualFunc selectChangedStateAvailableFunc(Random r, ConnectorState s) {
        List<ContextualFunc> curAvailableFunc = changedStateFuncMap.get(s);
        if (curAvailableFunc == null) {
            System.err.println("No available func to select in state: " + s.toString());
        }
        assert curAvailableFunc != null;
        int idx = r.nextInt(curAvailableFunc.size());
        return curAvailableFunc.get(idx);
    }
}

class FuncReader {
    public static void init(Map<ConnectorState, List<ContextualFunc>> sameStateFuncMap, Map<ConnectorState, List<ContextualFunc>> changedStateFuncMap,
                            List<ContextualFunc> startFunc, List<ContextualFunc> endFunc) {
        Map<String, Map<String, List<Method>>> basicInfo = new HashMap<>();
        for (Class<?> cls : Main.targetClass){
            Map<String, List<Method>> methodMap = new HashMap<>();
            for (Method method : cls.getMethods()) {
                String methodName = method.getName();
                List<Method> methods = methodMap.getOrDefault(methodName, new ArrayList<>());
                methods.add(method);
                methodMap.put(methodName, methods);
            }
            basicInfo.put(cls.getSimpleName(), methodMap);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(Main.jdbc))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split("\\$");
                addModeledFunc(info, basicInfo, sameStateFuncMap, changedStateFuncMap, startFunc, endFunc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addModeledFunc(String[] info, Map<String, Map<String, List<Method>>> basicInfo,
                                       Map<ConnectorState, List<ContextualFunc>> sameStateFuncMap, Map<ConnectorState, List<ContextualFunc>> changedStateFuncMap,
                                       List<ContextualFunc> startFunc, List<ContextualFunc> endFunc) {
        assert info.length == 5 && basicInfo.containsKey(info[0]);
        String cls = info[0];
        String mth = info[1];
        String[] params;
        if ("-".equals(info[2])) {
            params = new String[0];
        } else {
            params = info[2].split(",\\s+");
        }
        String preState = info[3];
        String nxtState = info[4];

        List<Method> methods = basicInfo.get(cls).get(mth);
        for (Method method : methods) {
            if (method.getParameters().length == params.length) {
                // filter out some execute methods
                if (method.getName().contains("execute") && method.getParameters().length == 2 && method.getParameters()[1].getType().isArray()) {
                    continue;
                }
                // filter out unsupported method in pg test
                if (Main.target.equals("postgres") && Main.oracle.equals("cp") && method.getName().contains("Large")) {
                    continue;
                }
                Set<ConnectorState> pStates = getPreStateFromDoc(preState);
                for (ConnectorState pState : pStates) {
                    ConnectorState nState = getNextStateFromDocAndPreState(nxtState, pState);
                    ContextualFunc func = new ContextualFunc(method, params, pState, nState);
                    if ("close".equals(method.getName())) {
                        endFunc.add(func);
                        continue;
                    }
                    if (pState.equals(new ConnectorState())) {
                        startFunc.add(func);
                        continue;
                    }

                    if (pState.equals(nState)) {
                        sameStateFuncMap.putIfAbsent(pState, new ArrayList<>());
                        sameStateFuncMap.get(pState).add(func);
                    } else {
                        changedStateFuncMap.putIfAbsent(pState, new ArrayList<>());
                        changedStateFuncMap.get(pState).add(func);
                    }
                }
            }
        }
    }

    private static Set<ConnectorState> getPreStateFromDoc(String state) {
        String[] multiStates = state.split("/\\s*");
        assert multiStates.length > 0;
        Set<ConnectorState> states = new HashSet<>();
        for (String multiState : multiStates) {
            String[] originStates = multiState.split(",\\s*");
            assert originStates.length == 3;
            List<ConnState> connStates = getConnStates(originStates[0]);
            List<StmtState> stmtStates = getStmtStates(originStates[1]);
            List<ResultState> resultStates = getResultStates(originStates[2]);
            assert !connStates.isEmpty() && !stmtStates.isEmpty() && !resultStates.isEmpty();
            for (ConnState connState : connStates) {
                for (StmtState stmtState : stmtStates) {
                    for (ResultState resultState : resultStates) {
                        states.add(new ConnectorState(connState, stmtState, resultState));
                    }
                }
            }
        }

        return states;
    }

    private static ConnectorState getNextStateFromDocAndPreState(String state, ConnectorState pState) {
        String[] originStates = state.split(",\\s+");
        assert !state.contains("/") && originStates.length == 3;
        ConnState connState;
        StmtState stmtState;
        ResultState resultState;

        List<ConnState> connStates = getConnStates(originStates[0]);
        List<StmtState> stmtStates = getStmtStates(originStates[1]);
        List<ResultState> resultStates = getResultStates(originStates[2]);
        assert !connStates.isEmpty() && !stmtStates.isEmpty() && !resultStates.isEmpty();
        // the next state is unique for one previous state
        if (connStates.size() > 1) {
            connState = pState.connState;
        } else {
            connState = connStates.get(0);
        }
        if (stmtStates.size() > 1) {
            stmtState = pState.stmtState;
        } else {
            stmtState = stmtStates.get(0);
        }
        if (resultStates.size() > 1) {
            resultState = pState.resultState;
        } else {
            resultState = resultStates.get(0);
        }

        return new ConnectorState(connState, stmtState, resultState);
    }

    private static List<ConnState> getConnStates(String state) {
        List<ConnState> connStates = new ArrayList<>();
        if ("Open".equals(state)) {
            connStates.add(ConnState.OPEN);
        } else if ("Close".equals(state)) {
            connStates.add(ConnState.CLOSE);
        } else if ("Not Nil".equals(state)) {
            connStates.add(ConnState.OPEN);
            connStates.add(ConnState.CLOSE);
        } else if ("-".equals(state)) {
            connStates.add(ConnState.OPEN);
            connStates.add(ConnState.CLOSE);
        }
        assert !connStates.isEmpty();
        return connStates;
    }

    private static List<StmtState> getStmtStates(String state) {
        List<StmtState> stmtStates = new ArrayList<>();
        if ("Created".equals(state)) {
            stmtStates.add(StmtState.CREATED);
        } else if ("Batch".equals(state)) {
            stmtStates.add(StmtState.BATCH);
        } else if ("Prepared".equals(state)) {
            stmtStates.add(StmtState.PREPARED);
        } else if ("Filled".equals(state)){
            stmtStates.add(StmtState.FILLED);
        } else if ("Executed".equals(state)) {
            stmtStates.add(StmtState.EXECUTED);
        } else if ("Close".equals(state)) {
            stmtStates.add(StmtState.CLOSE);
        } else if ("Not Close".equals(state)) {
            stmtStates.add(StmtState.CREATED);
            stmtStates.add(StmtState.PREPARED);
            stmtStates.add(StmtState.EXECUTED);
        } else if ("-".equals(state)) {
            stmtStates.add(StmtState.CREATED);
            stmtStates.add(StmtState.PREPARED);
            stmtStates.add(StmtState.EXECUTED);
            stmtStates.add(StmtState.CLOSE);
        }
        assert !stmtStates.isEmpty();
        return stmtStates;
    }

    private static List<ResultState> getResultStates(String state) {
        List<ResultState> resultStates = new ArrayList<>();
        if ("Created".equals(state)) {
            resultStates.add(ResultState.CREATED);
        } else if ("Scroll".equals(state)) {
            resultStates.add(ResultState.SCROLL);
        } else if ("Updated".equals(state)) {
            resultStates.add(ResultState.UPDATED);
        } else if ("Close".equals(state)) {
            resultStates.add(ResultState.CLOSE);
        } else if ("Not Close".equals(state)) {
            resultStates.add(ResultState.CREATED);
            resultStates.add(ResultState.SCROLL);
            resultStates.add(ResultState.UPDATED);
        } else if ("-".equals(state)) {
            resultStates.add(ResultState.CREATED);
            resultStates.add(ResultState.SCROLL);
            resultStates.add(ResultState.UPDATED);
            resultStates.add(ResultState.CLOSE);
        }
        assert !resultStates.isEmpty();
        return resultStates;
    }
}