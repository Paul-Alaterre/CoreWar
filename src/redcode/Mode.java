package redcode;
/**C'est la classe qui définit les différents modes d'adressages*/
public enum Mode {
    IMMEDIATE("#"),
    DIRECT(""),
    INDIRECT("@");

    /**Variable qui est le symbole associé au mode*/
    private final String symbol;

    /**Le constructeur qui associe le symbole au au mode*/
    Mode(String symbol) {
        this.symbol = symbol;
    }
    /**Une méthode pour récupérer le caractère associé au mode*/
    public String symbol() {
        return symbol;
    }

}
