package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /** Una persona leída del texto junto con el número de línea en el que estaba. */
    private static final class Linea {
        final int numero;
        final Persona persona;

        Linea(int numero, Persona persona) {
            this.numero = numero;
            this.persona = persona;
        }
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos la persona 1 (el sujeto).");
        }

        // 1. Leer cada línea (en el orden del texto) y convertirla en una Persona válida.
        List<Linea> lineas = new ArrayList<>();
        String[] renglones = texto.split("\\r?\\n", -1);
        for (int i = 0; i < renglones.length; i++) {
            if (!renglones[i].isBlank()) {
                lineas.add(new Linea(i + 1, leerPersona(renglones[i], i + 1)));
            }
        }

        // 2. Ninguna posición puede repetirse (se informa la segunda vez que aparece).
        for (int i = 0; i < lineas.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (lineas.get(i).persona.getPosicion().equals(lineas.get(j).persona.getPosicion())) {
                    throw new ArbolInvalidoException("Línea " + lineas.get(i).numero + ": la posición "
                            + lineas.get(i).persona.getPosicion() + " está repetida (ya estaba en la línea "
                            + lineas.get(j).numero + ").");
                }
            }
        }

        // 3. Ordenar para que cada persona se agregue después de la que necesita: el sujeto, los ascendientes
        //    por número Ahnentafel ascendente y los descendientes por generación. El sort es estable.
        lineas.sort((a, b) -> Long.compare(clave(a.persona.getPosicion()), clave(b.persona.getPosicion())));

        Linea primera = lineas.get(0);
        if (!Posicion.esSujeto(primera.persona.getPosicion())) {
            throw new ArbolInvalidoException("Falta la persona de la posición 1 (el sujeto del árbol).");
        }

        // 4. Armar el árbol. Las reglas del árbol (hijo/progenitor existente, años) las valida el árbol.
        ArbolGenealogico arbol = new ArbolGenealogico(primera.persona);
        for (Linea linea : lineas.subList(1, lineas.size())) {
            try {
                arbol.agregar(linea.persona);
            } catch (IllegalArgumentException e) {
                throw new ArbolInvalidoException("Línea " + linea.numero + ": " + e.getMessage());
            }
        }
        return arbol;
    }

    /**
     * Convierte una línea en una Persona válida.
     *
     * @throws ArbolInvalidoException con el prefijo "Línea N: " si la línea tiene algún problema
     */
    private static Persona leerPersona(String renglon, int numero) {
        String prefijo = "Línea " + numero + ": ";
        String[] campos = renglon.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw new ArbolInvalidoException(prefijo + "se esperaban 4 campos separados por ';' "
                    + "(posicion;nombre;nacimiento;fallecimiento) y hay " + campos.length + ".");
        }
        String posicion = campos[0].trim();
        String nombre = campos[1].trim();
        int nacimiento = leerAnio(campos[2].trim(), "nacimiento", prefijo);
        String textoFallecimiento = campos.length == 4 ? campos[3].trim() : "";
        Integer fallecimiento = textoFallecimiento.isEmpty() ? null : leerAnio(textoFallecimiento, "fallecimiento", prefijo);

        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw new ArbolInvalidoException(prefijo + e.getMessage());
        }
        return persona;
    }

    private static int leerAnio(String valor, String campo, String prefijo) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new ArbolInvalidoException(prefijo + "el año de " + campo + " debe ser un número entero (llegó \""
                    + valor + "\").");
        }
    }

    /**
     * Orden en el que se agregan las posiciones: el sujeto (0), los ascendientes por su número Ahnentafel
     * (2..2047) y después los descendientes por generación (cada generación necesita la anterior).
     */
    private static long clave(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            return Integer.parseInt(posicion);
        }
        return Posicion.MAX_AHNENTAFEL + Posicion.generacion(posicion);
    }
}
