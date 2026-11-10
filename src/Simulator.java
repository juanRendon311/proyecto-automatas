import java.util.ArrayList;
import java.util.List;

/**
 * Contiene las simulaciones de los cuatro modelos de la jerarquía de Chomsky.
 * Cada simulación guarda sus pasos para que la interfaz pueda reproducirlos.
 */
final class Simulator {
    /**
     * La interfaz usa estos datos para mostrar el estado, la cinta y el cabezal.
     */
    static final class Frame {
        final String state; // Estado activo, por ejemplo q0 o q_accept.
        final String note;  // Explicación de la transición realizada.
        final String tape;  // Cadena o contenido visible de la cinta.
        final int head;     // Posición del cabezal dentro de tape.
        final String extra; // Datos adicionales; aquí se guarda la pila del nivel 2.

        Frame(String state, String note, String tape, int head, String extra) {
            this.state = state;
            this.note = note;
            this.tape = tape;
            this.head = head;
            this.extra = extra;
        }
    }

    /** Resultado de una simulación: aceptación, mensaje final y pasos recorridos. */
    static final class Result {
        final boolean accepted; // true si la máquina acepta la entrada.
        final String message;   // Mensaje que se presenta al terminar.
        final List<Frame> frames; // Pasos en orden, desde el inicio hasta el resultado.

        Result(boolean accepted, String message, List<Frame> frames) {
            this.accepted = accepted;
            this.message = message;
            this.frames = frames;
        }
    }

    private Simulator() {
    }

    /**
     * Nivel 3: reconoce cadenas que terminan en el sufijo indicado.
     * El estado qN significa que N letras del inicio del sufijo coinciden
     * con el final de la parte de la cadena que ya se leyó.
     */
    static Result regular(String input, String suffix) {
        List<Frame> frames = new ArrayList<>();

        // Límites para que la animación no genere demasiados pasos.
        if (input.length() > 100 || suffix.length() > 8) {
            return failure("Límite de simulación: entrada de hasta 100 símbolos y sufijo de hasta 8.", frames);
        }

        // El patrón y la entrada deben usar el alfabeto {a, b}.
        if (suffix.isEmpty() || !suffix.matches("[ab]+")) {
            return failure("El sufijo debe contener una o más letras a o b.", frames);
        }
        if (!input.matches("[ab]*")) {
            return failure("La cadena solo puede contener a y b.", frames);
        }

        int state = 0;
        // El primer paso muestra la máquina antes de consumir la entrada.
        frames.add(new Frame("q0", "Estado inicial; no se ha leído ningún símbolo.",
                input, 0, "Estados: " + stateNames(suffix.length())));

        // Leer un símbolo puede completar el sufijo o dejar coincidente
        // solamente una parte más corta del patrón.
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            String candidate = suffix.substring(0, state) + symbol;

            // Si el patrón no coincide, retrocede por sus prefijos hasta
            // encontrar el prefijo más largo que todavía puede coincidir.
            while (state > 0 && !suffix.startsWith(candidate)) {
                state--;
                candidate = suffix.substring(0, state) + symbol;
            }

            // Si el candidato es prefijo del sufijo, ese es el nuevo estado.
            if (suffix.startsWith(candidate)) {
                state = candidate.length();
            } else {
                state = 0;
            }

            // Se guarda el estado y la posición para animar este paso.
            frames.add(new Frame("q" + state,
                    "Lee '" + symbol + "'; el mayor prefijo del sufijo coincidente tiene longitud " + state + ".",
                    input, i + 1, "Estados: " + stateNames(suffix.length())));
        }

        // Solo acepta si al terminar coincide el sufijo completo.
        boolean accepted = state == suffix.length();
        return new Result(accepted,
                accepted ? "Aceptada: la cadena termina en \"" + suffix + "\"."
                        : "Rechazada: la cadena no termina en \"" + suffix + "\".",
                frames);
    }

    private static String stateNames(int size) {
        List<String> names = new ArrayList<>();
        // Un sufijo de longitud N necesita los estados q0, q1, ..., qN.
        for (int i = 0; i <= size; i++) {
            names.add("q" + i);
        }
        return String.join(", ", names);
    }

    /**
     * Nivel 2: reconoce a^n b^n usando una pila LIFO.
     * Guarda una A por cada a y retira una A por cada b.
     */
    static Result pushdown(String input) {
        List<Frame> frames = new ArrayList<>();
        if (input.length() > 100) {
            return failure("La entrada no puede superar 100 símbolos.", frames);
        }
        // ⊥ marca el fondo y no se elimina durante la simulación.
        StringBuilder stack = new StringBuilder("⊥");
        frames.add(new Frame("q0", "Pila inicial con marcador de fondo ⊥.", input, 0, stack.toString()));

        // Cuando aparece el primer b, ya no se permiten más a.
        boolean readingBs = false;
        boolean valid = true;
        for (int i = 0; i < input.length(); i++) {
            char symbol = input.charAt(i);
            String state;
            String note;
            if (symbol == 'a' && !readingBs) {
                // Una a aumenta en uno la cantidad que deberá balancear la pila.
                stack.append('A');
                state = "q_push";
                note = "Lee 'a': apila A.";
            } else if (symbol == 'b') {
                // Al comenzar los b, cambiamos a la fase de desapilar.
                readingBs = true;
                if (stack.length() > 1) {
                    // El último carácter representa el tope de la pila.
                    stack.deleteCharAt(stack.length() - 1);
                    state = "q_pop";
                    note = "Lee 'b': desapila A.";
                } else {
                    valid = false;
                    state = "q_reject";
                    note = "No hay A disponible para desapilar.";
                }
            } else {
                // Rechaza a después de b y cualquier símbolo ajeno a {a, b}.
                valid = false;
                state = "q_reject";
                note = symbol == 'a' ? "No se permiten a después de comenzar los b."
                        : "Símbolo no perteneciente al alfabeto {a, b}.";
            }
            frames.add(new Frame(state, note, input, i + 1, stack.toString()));
            if (!valid) {
                return new Result(false, "Rechazada: " + note, frames);
            }
        }

        // Acepta si hubo al menos una a y un b, y solo queda el fondo.
        boolean accepted = !input.isEmpty() && readingBs && stack.length() == 1;
        return new Result(accepted,
                accepted ? "Aceptada: cada a tiene su b correspondiente (aⁿbⁿ, n ≥ 1)."
                        : "Rechazada: se requiere aⁿbⁿ con n ≥ 1 y pila vacía al terminar.",
                frames);
    }

    /**
     * Nivel 1: reconoce a^n b^n c^n en una cinta de longitud fija.
     * En cada vuelta marca una a, una b y una c que todavía no se hayan usado.
     */
    static Result boundedTape(String input) {
        List<Frame> frames = new ArrayList<>();
        if (input.length() > 100) {
            return failure("La entrada no puede superar 100 símbolos.", frames);
        }
        char[] tape = input.toCharArray(); // Solo las celdas de entrada; los límites se dibujan aparte.
        int head = 1; // Posición dentro de la cinta mostrada, que comienza con ⊢.
        frames.add(new Frame("q_start", "Cinta delimitada; el cabezal está en el primer símbolo.",
                boundedString(tape), head, ""));

        // Se exige una entrada no vacía formada por a, b y c.
        if (!input.matches("[abc]+")) {
            frames.add(new Frame("q_reject", "Entrada vacía o con símbolos fuera de {a, b, c}.",
                    boundedString(tape), head, ""));
            return new Result(false, "Rechazada: se requiere una cadena no vacía sobre {a, b, c}.", frames);
        }

        int loops = 0;
        // Como mínimo cada vuelta marca una a; este límite protege la simulación.
        while (loops++ <= tape.length) {
            head = 1;

            // Busca la siguiente a sin marcar. Al llegar al final, ya no quedan a.
            while (head <= tape.length && tape[head - 1] != 'a') {
                head++;
                if (head <= tape.length) {
                    frames.add(new Frame("q_find_a", "Busca una a sin marcar hacia la derecha.",
                            boundedString(tape), head, ""));
                }
            }
            if (head > tape.length) {
                // Si todas las celdas están marcadas, las cantidades eran iguales.
                boolean allMarked = true;
                for (char cell : tape) {
                    if (cell != 'X' && cell != 'Y' && cell != 'Z') {
                        allMarked = false;
                        break;
                    }
                }
                frames.add(new Frame(allMarked ? "q_accept" : "q_reject",
                        allMarked ? "No quedan símbolos sin marcar." : "Quedaron símbolos sin pareja.",
                        boundedString(tape), tape.length + 1, ""));
                return new Result(allMarked,
                        allMarked ? "Aceptada: pertenece a {aⁿbⁿcⁿ | n ≥ 1}."
                                : "Rechazada: las cantidades o el orden de a, b y c no coinciden.",
                        frames);
            }

            // Marca la a elegida con X.
            tape[head - 1] = 'X';
            frames.add(new Frame("q_mark_a", "Marca una a como X.", boundedString(tape), head, ""));
            head++;

            // Avanza por el bloque de a marcadas hasta hallar una b sin marcar.
            while (head <= tape.length && tape[head - 1] != 'b') {
                // Si aparece c o Z antes que una b, el orden está roto.
                if (tape[head - 1] != 'a' && tape[head - 1] != 'X' && tape[head - 1] != 'Y') {
                    return boundedReject(frames, tape, head, "No se encontró una b para la a marcada.");
                }
                head++;
                if (head <= tape.length) {
                    frames.add(new Frame("q_find_b", "Busca la siguiente b sin marcar.",
                            boundedString(tape), head, ""));
                }
            }
            if (head > tape.length) {
                return boundedReject(frames, tape, tape.length + 1, "No hay una b disponible.");
            }
            tape[head - 1] = 'Y';
            frames.add(new Frame("q_mark_b", "Marca una b como Y.", boundedString(tape), head, ""));
            head++;

            // Después busca una c sin marcar, pasando por b ya emparejadas.
            while (head <= tape.length && tape[head - 1] != 'c') {
                if (tape[head - 1] != 'b' && tape[head - 1] != 'Y' && tape[head - 1] != 'Z') {
                    return boundedReject(frames, tape, head, "No se encontró una c para la b marcada.");
                }
                head++;
                if (head <= tape.length) {
                    frames.add(new Frame("q_find_c", "Busca la siguiente c sin marcar.",
                            boundedString(tape), head, ""));
                }
            }
            if (head > tape.length) {
                return boundedReject(frames, tape, tape.length + 1, "No hay una c disponible.");
            }
            tape[head - 1] = 'Z';
            frames.add(new Frame("q_mark_c", "Marca una c como Z; vuelve al inicio para otra ronda.",
                    boundedString(tape), head, ""));

            // Vuelve a la frontera izquierda para comenzar la siguiente pareja.
            while (head > 0) {
                head--;
                frames.add(new Frame("q_return", "Mueve el cabezal a la izquierda hacia ⊢.",
                        boundedString(tape), head, ""));
            }
            head++;
            frames.add(new Frame("q_return", "Cruza ⊢ hacia la primera celda de entrada.",
                    boundedString(tape), head, ""));
        }
        return boundedReject(frames, tape, head, "Se alcanzó el límite de seguridad de la simulación.");
    }

    private static String boundedString(char[] tape) {
        // Los delimitadores se muestran, pero no ocupan posiciones del arreglo.
        return "⊢" + new String(tape) + "⊣";
    }

    private static Result boundedReject(List<Frame> frames, char[] tape, int head, String note) {
        // Centraliza el registro de los distintos motivos de rechazo del nivel 1.
        frames.add(new Frame("q_reject", note, boundedString(tape), head, ""));
        return new Result(false, "Rechazada: " + note, frames);
    }

    /**
     * Nivel 0: Máquina de Turing que reconoce 0^n 1^n.
     * Marca cada 0 como X, busca un 1 y lo marca como Y; luego vuelve a la izquierda.
     */
    static Result turing(String input) {
        List<Frame> frames = new ArrayList<>();
        if (input.length() > 100) {
            return failure("La entrada no puede superar 100 símbolos.", frames);
        }
        // Tres blancos a la izquierda permiten visualizar el regreso del cabezal.
        List<Character> tape = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tape.add('□');
        }
        for (char symbol : input.toCharArray()) {
            tape.add(symbol);
        }
        tape.add('□');
        int head = 3; // La entrada comienza en la cuarta celda, después de los blancos.
        String state = "q0";
        boolean paired = false;
        frames.add(new Frame(state, "Cinta inicial; alfabeto de cinta {0, 1, X, Y, □}.",
                tapeString(tape), head, ""));
        if (!input.matches("[01]*")) {
            frames.add(new Frame("q_reject", "Símbolo fuera del alfabeto de entrada {0, 1}.",
                    tapeString(tape), head, ""));
            return new Result(false, "Rechazada: la entrada solo puede contener 0 y 1.", frames);
        }

        // q0 busca un 0, q1 busca el 1 correspondiente, q_return vuelve al inicio,
        // y q_check verifica al final que no hayan quedado símbolos sin emparejar.
        for (int steps = 0; steps < 5000; steps++) {
            char symbol = tape.get(head);
            String note;
            switch (state) {
                case "q0":
                    if (symbol == 'X') {
                        // Este 0 ya fue emparejado, así que continúa a la derecha.
                        head++;
                        note = "Salta un 0 ya marcado.";
                    } else if (symbol == '0') {
                        // Reserva este 0 y cambia al estado que busca un 1.
                        tape.set(head, 'X');
                        head++;
                        state = "q1";
                        note = "Marca el 0 como X y busca su pareja 1.";
                    } else {
                        // Si no hay más ceros, pasa a verificar el resto de la cinta.
                        state = "q_check";
                        note = "No hay más 0 sin marcar; verifica que no queden 1.";
                    }
                    break;
                case "q1":
                    if (symbol == '0' || symbol == 'Y') {
                        // Todavía no encontró el 1 sin marcar que corresponde al 0.
                        head++;
                        note = "Avanza buscando un 1 sin marcar.";
                    } else if (symbol == '1') {
                        // Encuentra y marca el 1; ahora debe volver al extremo izquierdo.
                        tape.set(head, 'Y');
                        head--;
                        state = "q_return";
                        paired = true;
                        note = "Marca el 1 como Y y regresa al inicio.";
                    } else {
                        return turingReject(frames, tape, head, "No existe un 1 para emparejar el 0.");
                    }
                    break;
                case "q_return":
                    if (symbol == '□') {
                        // Encontró el blanco de la izquierda y empieza otra búsqueda.
                        head++;
                        state = "q0";
                        note = "Llega al blanco izquierdo y vuelve a buscar otro 0.";
                    } else {
                        // Retrocede celda por celda hasta el blanco izquierdo.
                        head--;
                        note = "Se desplaza hacia el blanco del extremo izquierdo.";
                    }
                    break;
                case "q_check":
                    if (symbol == 'X' || symbol == 'Y') {
                        // Las marcas son parejas ya revisadas; continúa comprobando.
                        head++;
                        note = "Símbolo ya emparejado; continúa la verificación.";
                    } else if (symbol == '□') {
                        // Llegó al blanco derecho sin encontrar símbolos pendientes.
                        boolean accepted = paired;
                        state = accepted ? "q_accept" : "q_reject";
                        note = accepted ? "Todos los símbolos están emparejados." : "Se requiere al menos una pareja.";
                        frames.add(new Frame(state, note, tapeString(tape), head, ""));
                        return new Result(accepted,
                                accepted ? "Aceptada por la Máquina de Turing (0ⁿ1ⁿ, n ≥ 1)."
                                        : "Rechazada: el lenguaje requiere n ≥ 1.",
                                frames);
                    } else {
                        return turingReject(frames, tape, head, "Quedó un símbolo sin pareja o en orden incorrecto.");
                    }
                    break;
                default:
                    return turingReject(frames, tape, head, "Estado de máquina no reconocido.");
            }
            ensureCell(tape, head);
            frames.add(new Frame(state, note, tapeString(tape), head, ""));
        }
        return turingReject(frames, tape, head, "Se alcanzó el límite de seguridad de la simulación.");
    }

    private static void ensureCell(List<Character> tape, int head) {
        if (head < 0) {
            // La cinta de Turing también puede crecer por el extremo izquierdo.
            tape.add(0, '□');
            return;
        }
        while (head >= tape.size()) {
            // Añade blancos hasta que exista la celda a la que apunta el cabezal.
            tape.add('□');
        }
    }

    private static String tapeString(List<Character> tape) {
        StringBuilder value = new StringBuilder();
        // Convierte las celdas en texto para guardarlas en el paso de animación.
        for (char symbol : tape) {
            value.append(symbol);
        }
        return value.toString();
    }

    private static Result turingReject(List<Frame> frames, List<Character> tape, int head, String note) {
        // Mantiene la posición del cabezal dentro de la cinta al mostrar el rechazo.
        head = Math.max(0, Math.min(head, tape.size() - 1));
        frames.add(new Frame("q_reject", note, tapeString(tape), head, ""));
        return new Result(false, "Rechazada: " + note, frames);
    }

    private static Result failure(String message, List<Frame> frames) {
        // Se usa cuando la entrada es inválida antes de iniciar la máquina.
        return new Result(false, message, frames);
    }
}
