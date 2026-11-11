# Proyecto de autómatas: Jerarquía de Chomsky

Aplicación en Java Swing para visualizar y simular modelos computacionales de los cuatro niveles de la jerarquía de Chomsky. La interfaz está en español, incluye ejemplos y no requiere dependencias externas.

## Requisitos

- JDK 8 o posterior.
- Windows, macOS o Linux con entorno gráfico.

## Instalación y ejecución

Desde la carpeta raíz del proyecto:

```powershell
javac -encoding UTF-8 -d bin src\App.java src\Simulator.java
java -cp bin App
```

En VS Code también se puede abrir `src\App.java` y ejecutar `App`. Selecciona un nivel en las pestañas de colores. La interfaz usa tarjetas redondeadas, una paleta de alto contraste y controles agrupados. Cada módulo dibuja la transición actual entre estados y muestra la cinta o cadena, resalta el cabezal y las marcas, y permite cargar ejemplos, avanzar o retroceder, reproducir, pausar, cambiar la velocidad y reiniciar.

Para compilar y ejecutar las pruebas de los simuladores:

```powershell
javac -encoding UTF-8 -d bin src\App.java src\Simulator.java test\SimulatorTest.java
java -cp bin SimulatorTest
```

## Módulos y cadenas de prueba

| Nivel | Modelo y lenguaje | Ejemplos incluidos |
|---|---|---|
| 3 | AFD para cadenas sobre `{a,b}` que terminan en un sufijo configurable. El sufijo inicial es `ab`. | `aab` (acepta), `abba` (rechaza), `abab` (acepta), `baa` (rechaza). |
| 2 | Autómata de pila para `{aⁿbⁿ \| n ≥ 1}`. Apila una `A` por cada `a` y la desapila por cada `b`. | `aaabbb` (acepta), `aabb` (acepta), `aabbb` (rechaza), `abab` (rechaza). |
| 1 | Autómata linealmente acotado para `{aⁿbⁿcⁿ \| n ≥ 1}`. Marca los símbolos en una cinta entre `⊢` y `⊣`. | `aabbcc` (acepta), `abc` (acepta), `aabcc` (rechaza), `abcc` (rechaza). |
| 0 | Máquina de Turing para `{0ⁿ1ⁿ \| n ≥ 1}`. Marca parejas `0`/`1` en una cinta extensible con blanco `□`. | `0011` (acepta), `000111` (acepta), `001` (rechaza), `0101` (rechaza). |

Para cambiar el sufijo del módulo de nivel 3, escribe una secuencia no vacía de `a` y `b` en **Sufijo objetivo**. Las simulaciones admiten hasta 100 símbolos de entrada; el sufijo configurable admite hasta 8.

## Guion breve para la exposición

- **Nivel 3:** cada estado recuerda cuántas letras iniciales del sufijo coinciden con el final de lo leído.
- **Nivel 2:** cada `a` agrega una `A` a la pila y cada `b` quita una; la pila debe quedar en el marcador de fondo.
- **Nivel 1:** el cabezal marca en cada ronda una `a`, una `b` y una `c`; acepta cuando no quedan letras sin marcar.
- **Nivel 0:** la máquina marca un `0`, busca su `1`, regresa al inicio y repite hasta verificar todas las parejas.
- **Interfaz:** cada pestaña comparte un panel. Simular calcula los pasos, Siguiente paso los muestra uno a uno y Reproducir los recorre automáticamente.

## Publicar en GitHub

El proyecto ya tiene configurado su repositorio remoto. Para subir cambios nuevos, ejecuta desde la carpeta del proyecto:

```powershell
git add README.md src\App.java src\Simulator.java docs\IMPLEMENTACION.md test\SimulatorTest.java
git commit -m "Describe tus cambios"
git push
```

## Documento técnico

La explicación de los modelos, los algoritmos de simulación y las decisiones de interfaz está en [`docs/IMPLEMENTACION.md`](docs/IMPLEMENTACION.md). la cui
