package context.schema;

import java.util.List;
import java.util.Random;

public class Table {
    private String name;
    private List<Column> cols;
    private Column idCol;
    private Column valueCol;

    public String getName() {
        return name;
    }

    public List<Column> getCols() {
        return cols;
    }

    public Column getIdCol() {
        return idCol;
    }

    public Column getValueCol() {
        return valueCol;
    }

    public Column getRandomColumn(Random r) {
        return cols.get(r.nextInt(cols.size()));
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Table(String name, List<Column> cols) {
        this.name = name;
        this.cols = cols;
        for (Column col : cols) {
            col.setTable(this);
            if (col.isId()) {
                this.idCol = col;
            }
            if (col.isValue()) {
                this.valueCol = col;
            }
        }
    }
}
