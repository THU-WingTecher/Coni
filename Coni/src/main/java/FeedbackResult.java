import input.ContextualFunc;

import java.util.Arrays;

public class FeedbackResult {
    private String id;
    private ContextualFunc func;
    private Object[] args;
    private Object result;
    private String resultValue;
    public boolean isError = false;
    private boolean isCreated = false;

    public String getReproduceInfo() {
        String cls = this.func.getMethod().getDeclaringClass().getSimpleName();
        String method = this.func.getMethod().getName();
        String object;

        if ("DriverManager".equals(cls)) {
            object = "DriverManager";
        } else if ("Connection".equals(cls)) {
            object = "con";
        } else if (cls.contains("Statement")) {
            object = "stmt";
        } else if ("ResultSet".equals(cls)) {
            object = "rs";
        } else if ("Savepoint".equals(cls)) {
            object = "sp";
        } else {
            throw new IllegalArgumentException("Unsupported class: " + cls);
        }

        StringBuffer arg = new StringBuffer();
        if ("setObject".equals(method) && "stmt".equals(object)) {
            String tmplate =  object + "." + method + "(%s, %s);\n ";
            for (int i = 0; i < args.length; i++) {
                String cur = (String) args[i];
                if (cur == null) {
                    continue;
                }
                String[] parts = cur.substring(1, cur.length() - 1).split(", ");
                assert parts.length == 2;
                arg.append(String.format(tmplate, parts[0], "\""+ parts[1] + "\""));
            }
            return arg.toString();
        } else {
            for (int i = 0; i < args.length; i++) {
                Object o = args[i];
                if (i > 0) {
                    arg.append(", ");
                }
                if (o instanceof String) {
                    arg.append("\"");
                    arg.append(o);
                    arg.append("\"");
                } else {
                    arg.append(o);
                }
            }
        }

        String call = object + "." + method + "(" + arg + ")";
        if (this.result == null) {
            return call + ";";
        } else if ("No ResultSet object".equals(this.resultValue)) {
            return "";
        } else if (this.isError) {
            StringBuffer tmp = new StringBuffer("try {\n");
            tmp.append("\t" + call + ";\n");
            tmp.append("} catch (Exception e) {\n");
            tmp.append("\tSystem.out.println(e);\n");
            tmp.append("}");
            return tmp.toString();
        } else if (this.result.getClass().isArray()) {
            return "System.out.println(Arrays.toString(" + call + "));";
        } else if (this.resultValue.contains("Connection")) {
            return "con = " + call + ";";
        } else if (this.resultValue.contains("Statement")) {
            return "stmt = " + call + ";";
        } else if (this.resultValue.contains("Result")) {
            return "rs = " + call + ";";
        } else if (this.resultValue.contains("Savepoint")) {
            return "sp = " + call + ";";
        }
        else {
            return "System.out.println("+ call + ");";
        }
    }

    public String getExecuteInfo() {
        String cls = this.func.getMethod().getDeclaringClass().getSimpleName();
        String method = this.func.getMethod().getName();
        String args = Arrays.toString(this.args);
        return cls + "." + method + ": " + args;
    }

    public FeedbackResult(String id, ContextualFunc func, Object[] args, Object result) {
        this.id = id;
        this.func = func;
        this.args = args;
        this.result = result;
        this.resultValue = setResultValue(this.result);
    }

    public FeedbackResult(String id, ContextualFunc func, Object[] args, Object result, boolean create) {
        this.id = id;
        this.func = func;
        this.args = args;
        this.result = result;
        this.resultValue = setResultValue(this.result);
        this.isCreated = create;
    }

    private String setResultValue(Object result) {
        if (result == null) {
            return "null";
        } else if (result.getClass().isArray()) {
            if (result instanceof int[]) {
                return Arrays.toString((int[]) result);
            } else if (result instanceof long[]) {
                return Arrays.toString((long[]) result);
            } else if (result instanceof String[]) {
                return Arrays.toString((String[]) result);
            } else {
                throw new IllegalArgumentException("Unsupported array type: " + result.getClass().getName());
            }
        } else if (result instanceof Throwable) {
            this.isError = true;
            return result.toString();
        } else {
            return result.toString();
        }
    }

    public String getMethodName(){
        String cls = this.func.getMethod().getDeclaringClass().getSimpleName();
        String method = this.func.getMethod().getName();
        return cls + "." + method;
    }

    public String getResultValue() {
        return id + ": " + this.resultValue;
    }

    public ContextualFunc getFunc() {
        return func;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeedbackResult r = (FeedbackResult) o;
        return this.func.equals(r.func) && this.isError == r.isError && this.isCreated == r.isCreated &&
                (this.isError || this.isCreated || this.resultValue.equals(r.resultValue));
    }
}
