package state;

import java.util.Objects;

public class ConnectorState {
    public ConnState connState;
    public StmtState stmtState;
    public ResultState resultState;

    public ConnectorState() {
        this.connState = ConnState.CLOSE;
        this.stmtState = StmtState.CLOSE;
        this.resultState = ResultState.CLOSE;
    }

    public ConnectorState(ConnState connState, StmtState stmtState, ResultState resultState) {
        this.connState = connState;
        this.stmtState = stmtState;
        this.resultState = resultState;
    }

    @Override
    public String toString() {
        return this.connState.toString() + ", " + this.stmtState.toString() + ", " + this.resultState.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectorState cs = (ConnectorState) o;
        return this.connState == cs.connState && this.stmtState == cs.stmtState && this.resultState == cs.resultState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.connState, this.stmtState, this.resultState);
    }
}



