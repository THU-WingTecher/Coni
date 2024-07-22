import java.io.*;
import java.util.*;

public class ResultMonitor {
    private String id;
    private long seed;
    private List<FeedbackResult> res1;
    private List<FeedbackResult> res2;
    private List<String> prefix;
    private List<String> bufferedLog;
    private String type;
    private static Set<String> blackList = new HashSet<>();

    public boolean checkResult() {
        boolean isSame = true;
        int correct = 0;
        if (res1 == null && res2 == null) {
            return isSame;
        }
        else if (res1 == null || res2 == null) {
            throw new IllegalArgumentException("res1 and res2 should be both null or not null");
        }
        else if (res1.size() != res2.size()) {
            throw new IllegalArgumentException("res1 and res2 should have same size");
        } else {
            int size = res1.size();
            for (int i = 0; i < size; i++) {
                FeedbackResult r1 = res1.get(i);
                FeedbackResult r2 = res2.get(i);
                if (!r1.isError && !r2.isError) {
                    correct++;
                }
                if (!r1.equals(r2) && !blackList.contains(r1.getMethodName()) && !blackList.contains(r2.getMethodName())) {
                    assert r1.getFunc().equals(r2.getFunc());
                    isSame = false;
                    bufferedLog.add("\nSystem.out.println(\"ERROR:\");");
                    if (type == null) {
                        type = r1.getMethodName().toLowerCase();
                    }
                }

                String log1 = r1.getReproduceInfo();
                String log2 = r2.getReproduceInfo();
                if (log2.startsWith("try") && !log1.startsWith("try")) {
                    log1 = "try {\n\t" + log1 + "\n} catch (Exception e) {\n\tSystem.out.println(e);\n}";
                } else if (!log1.equals(log2) && !log2.startsWith("try") && !log1.startsWith("try")) {
                    bufferedLog.add("// " + log2);
                }
                bufferedLog.add(log1);
            }
        }
        if (!isSame) {
            saveBug(this.bufferedLog);
        }
        Main.sharedCorrectNum.addAndGet(correct);
        return isSame;
    }

    public static boolean initBlackList(String type) {
        String path = Main.blacklist;
        switch (type) {
            case "mysql":
                path += "/mysql.txt";
                break;
            case "postgresql":
            case "postgres":
                path += "/postgres.txt";
                break;
            default:
                System.err.println(String.format("Unknown blacklist type: %s", type));
                return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                blackList.add(line.strip());
            }
        } catch (IOException e) {
            System.err.println("Error when reading blacklist");
            return false;
        }
        return true;
    }

    private void addInit(List<String> bufferedLog) {
        bufferedLog.add("// seed: " + seed);
        bufferedLog.add(" ");
        bufferedLog.add("Connection con = null;");
        bufferedLog.add("Statement stmt = null;");
        bufferedLog.add("ResultSet rs = null;");
        bufferedLog.add("Savepoint sp = null;");
    }

    private void saveBug(List<String> bufferedLog) {
        String dict =  type != null ? type : "unknown";
        String path = Main.out + dict;
        String name = "/" + id;

        File folder = new File(path);
        if (!folder.exists()) {
            Main.sharedBugNum.incrementAndGet();
            folder.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path + name))) {
            for (String line : prefix) {
                writer.write(line + "\n");
            }
            for (String line : bufferedLog) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.err.println(String.format("Error when writing %s, e: %s", path + name, e));
        }
    }

    public ResultMonitor(String id, long seed, List<FeedbackResult> res1, List<FeedbackResult> res2, Random r1, Random r2) {
        this.id = id;
        this.seed = seed;
        this.res1 = res1;
        this.res2 = res2;
        this.bufferedLog = new ArrayList<>();
        this.prefix = new ArrayList<>();
        if (r1.nextInt() != r2.nextInt()) {
            prefix.add("// seed seems not equal somewhere");
        }
        addInit(this.bufferedLog);
    }
}
