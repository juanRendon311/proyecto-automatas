public class SimulatorTest {
    public static void main(String[] args) {
        expect(Simulator.regular("aab", "ab"), true, "AFD accepts matching suffix");
        expect(Simulator.regular("aba", "ab"), false, "AFD rejects non-matching suffix");
        expect(Simulator.regular("abbaa", "baa"), true, "AFD supports configured suffix");
        expect(Simulator.regular("abc", "ab"), false, "AFD rejects invalid alphabet");

        expect(Simulator.pushdown("aaabbb"), true, "PDA accepts equal a and b counts");
        expect(Simulator.pushdown("aabbb"), false, "PDA rejects extra b");
        expect(Simulator.pushdown("abab"), false, "PDA rejects invalid order");
        expect(Simulator.pushdown(""), false, "PDA rejects empty string");

        expect(Simulator.boundedTape("abc"), true, "LBA accepts n=1");
        expect(Simulator.boundedTape("aabbcc"), true, "LBA accepts n=2");
        expect(Simulator.boundedTape("aaabbbccc"), true, "LBA accepts n=3");
        expect(Simulator.boundedTape("aabcc"), false, "LBA rejects unequal counts");
        expect(Simulator.boundedTape("acb"), false, "LBA rejects invalid order");

        expect(Simulator.turing("01"), true, "Turing machine accepts n=1");
        expect(Simulator.turing("0011"), true, "Turing machine accepts n=2");
        expect(Simulator.turing("000111"), true, "Turing machine accepts n=3");
        expect(Simulator.turing("001"), false, "Turing machine rejects unequal counts");
        expect(Simulator.turing("0101"), false, "Turing machine rejects invalid order");
        expect(Simulator.turing(""), false, "Turing machine rejects empty string");
        expect(Simulator.turing("01x"), false, "Turing machine rejects invalid alphabet");

        System.out.println("Todas las pruebas del simulador pasaron.");
    }

    private static void expect(Simulator.Result result, boolean expected, String description) {
        if (result.accepted != expected) {
            throw new AssertionError(description + ": se esperaba " + expected
                    + ", resultado=" + result.accepted + ", mensaje=" + result.message);
        }
        for (Simulator.Frame frame : result.frames) {
            if (frame == null) {
                throw new AssertionError(description + ": se encontró una configuración nula.");
            }
        }
    }
}
