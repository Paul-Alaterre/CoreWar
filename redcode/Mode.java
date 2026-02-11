package redcode;

public enum Mode {
    IMMEDIATE("#"),
    DIRECT(""),
    INDIRECT("@");

    private final String symbol;

    Mode(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}