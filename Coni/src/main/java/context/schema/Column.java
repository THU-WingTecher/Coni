package context.schema;

public class Column {
    private String name;
    private String type;
    private Table table;
    private boolean isId = false;
    private boolean isValue = true;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public boolean isId() {
        return isId;
    }

    public boolean isValue() {
        return isValue;
    }

    @Override
    public String toString() {
        return this.table.toString() + "." + this.name;
    }

    public Column(String name, String type) {
        this.name = name;
        this.type = type;
        if ("id".equalsIgnoreCase(name)) {
            this.isId = true;
            this.isValue = false;
        }
    }
}
