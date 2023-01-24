/**
 * A class representing a value in a three-valued logic system, where the third value is `UNKNOWN`.
 */
public enum Logic {
    TRUE("1"),
    FALSE("0"),
    UNKNOWN("U");
    // this value's string representation
    private final String string;

    // initialises `string`
    Logic(String string) {
        this.string = string;
    }

    /**
     * Negates a value.
     * @param v a value
     * @return the negation of the given value
     */
    public static Logic NOT(Logic v) {
        return switch (v) {
            case TRUE -> Logic.FALSE;
            case FALSE -> Logic.TRUE;
            default -> Logic.UNKNOWN;
        };
    }

    /**
     * Calculates the disjunction of two values.
     * @param v1 a 3VL value
     * @param v2 a 3VL value
     * @return the disjunction of those two values
     */
    public static Logic OR(Logic v1, Logic v2) {
        if (v1 == Logic.TRUE || v2 == Logic.TRUE) {
            return Logic.TRUE;
        } else if (v1 == Logic.FALSE && v2 == Logic.FALSE) {
            return Logic.FALSE;
        } else {
            return Logic.UNKNOWN;
        }
    }

    /**
     * Calculates the conjunction of two values.
     * @param v1 a 3VL value
     * @param v2 a 3VL value
     * @return the conjunction of those two values
     */
    public static Logic AND(Logic v1, Logic v2) {
        if (v1 == Logic.FALSE || v2 == Logic.FALSE) {
            return Logic.FALSE;
        } else if (v1 == Logic.TRUE && v2 == Logic.TRUE) {
            return Logic.TRUE;
        } else {
            return Logic.UNKNOWN;
        }
    }

    /**
     * Calculates the exclusive disjunction of two values.
     * @param v1 a 3VL value
     * @param v2 a 3VL value
     * @return the exclusive disjunction of those two values
     */
    public static Logic XOR(Logic v1, Logic v2) {
        return OR(AND(v1, NOT(v2)), AND(NOT(v1), v2));
    }

    @Override
    public String toString() {
        return string;
    }
}
