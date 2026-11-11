# Implementación y conceptos

## Arquitectura

El programa está desarrollado en Java estándar y Swing. `App.java` crea una ventana con pestañas: cada pestaña comparte un panel con tarjetas redondeadas, una cabecera de alto contraste, la entrada, los ejemplos y los controles de simulación. Una vista gráfica dibuja los estados o las celdas, resalta el cabezal y colorea las marcas; una barra de progreso y un historial ayudan a seguir la ejecución. Los botones permiten avanzar, retroceder, reproducir, pausar, cambiar la velocidad y reiniciar. `Simulator.java` implementa los modelos y devuelve una secuencia de configuraciones de ejecución (`Frame`); cada configuración conserva su estado, descripción de la transición, cinta/cadena, posición del cabezal y, para el autómata de pila, contenido de la pila.

La interfaz presenta una configuración a la vez. **Avanzar paso** muestra la siguiente; **Reproducir** usa un temporizador de Swing para recorrerlas automáticamente. Los simuladores se ejecutan en el hilo de eventos, por lo que se limita cada entrada a 100 símbolos y el sufijo del AFD a 8 para mantener la interfaz interactiva.

## Nivel 3: lenguajes regulares y AFD

Se reconoce el lenguaje de cadenas sobre `{a,b}` que terminan en un sufijo configurable. El AFD tiene estados `q0` hasta `qm`, donde `m` es la longitud del sufijo. El estado `qi` representa que los últimos símbolos leídos coinciden con los primeros `i` símbolos del sufijo. Para cada símbolo, se calcula el mayor prefijo del sufijo que coincide con el final de lo leído; `qm` es el estado de aceptación.

El dibujo muestra el estado de origen y el estado de destino de cada transición, con una flecha resaltada; debajo se indica la posición de lectura en la cadena. El sufijo debe contener al menos un símbolo y solo puede usar `a` y `b`.

## Nivel 2: lenguaje libre de contexto y pila

El lenguaje es `{aⁿbⁿ | n ≥ 1}`. En el estado inicial/de lectura de `a`, cada `a` apila `A`. Al comenzar los `b`, cada `b` desapila un `A`; no se permiten `a` después de un `b`. Se acepta si se leyó al menos un `b`, la entrada terminó y solo permanece el símbolo de fondo `⊥`. Una cadena con orden incorrecto, demasiados `b` o símbolos inválidos se rechaza.

La visualización incluye el diagrama de la transición actual, la entrada y el contenido de la pila, con el tope a la derecha.

## Nivel 1: lenguaje sensible al contexto y cinta acotada

El lenguaje es `{aⁿbⁿcⁿ | n ≥ 1}`. La cinta tiene exactamente la longitud de entrada, más los delimitadores `⊢` y `⊣`; el cabezal permanece dentro de esos límites. En cada ronda se marca una `a` sin marcar como `X`, una `b` como `Y` y una `c` como `Z`. La búsqueda respeta el orden de bloques y no puede cruzar un delimitador. Al terminar las rondas se acepta únicamente si todas las celdas están marcadas.

El diagrama resalta la transición actual y el recorrido y las marcas se muestran sobre la cinta en cada configuración.

## Nivel 0: Máquina de Turing

La máquina reconoce `{0ⁿ1ⁿ | n ≥ 1}` usando el alfabeto de cinta `{0, 1, X, Y, □}`. Marca el siguiente `0` como `X`, busca y marca su pareja `1` como `Y`, y regresa al extremo izquierdo para repetir. Cuando ya no quedan `0` sin marcar, verifica que tampoco queden símbolos sin emparejar. Los blancos `□` delimitan la parte utilizada; la cinta añade celdas en blanco si el cabezal avanza más allá del extremo visible.

La vista presenta el diagrama de la transición actual, las celdas de cinta, el cabezal y el estado activo. Las transiciones de rechazo informan si falta una pareja, hay símbolos fuera del alfabeto o el orden no corresponde al lenguaje.

## Validación de entradas

Las cadenas inválidas se rechazan con una explicación visible en la interfaz. Los módulos de pila, cinta acotada y Máquina de Turing exigen entradas de hasta 100 símbolos. El AFD limita el sufijo a 8 símbolos y la entrada a 100. Los límites evitan crear animaciones excesivamente largas en la interfaz gráfica.
