package markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas unitarias y de integración con JUnit 5 (Jupiter)
 * para verificar la aplicación {@link ConversorXmlMarkdownHtml}.
 * Cubre funcionamiento normal, casos límite, entradas incorrectas y ficheros inexistentes.
 */
class ConversorXmlMarkdownHtmlTest {

    @TempDir
    Path tempDir;

    /**
     * CP-01: Funcionamiento normal.
     * Comprueba que el fichero original productos.xml se lee correctamente (4 productos),
     * calcula bien los totales (45 unidades, 7289.55 €) y genera tanto el Markdown como el HTML con tabla.
     */
    @Test
    @DisplayName("CP-01: Funcionamiento normal - Lectura de XML y generación de Markdown y HTML")
    void testFuncionamientoNormal() {
        File xmlFile = new File("src/main/java/markdown/productos.xml");
        assertTrue(xmlFile.exists(), "El fichero productos.xml debe existir en el proyecto");

        // 1. Verificar lectura de productos desde XML
        List<Producto> productos = ConversorXmlMarkdownHtml.leerProductosDesdeXml(xmlFile);
        assertEquals(4, productos.size(), "Deben haberse leído exactamente 4 productos");
        assertEquals("Portátil Gaming Asus", productos.get(0).getNombre());
        assertEquals(4499.95, productos.get(0).getValorStock(), 0.01);

        // 2. Verificar generación del informe Markdown (.md)
        String md = ConversorXmlMarkdownHtml.generarMarkdown(productos);
        assertTrue(md.contains("# Catálogo de Productos"), "El Markdown debe incluir el título principal");
        assertTrue(md.contains("**Total de productos (referencias):** 4"), "Debe indicar 4 referencias en el resumen");
        assertTrue(md.contains("**Unidades totales en stock:** 45"), "Debe indicar 45 unidades totales");
        assertTrue(md.contains("7289,55") || md.contains("7289.55"), "Debe contener el valor total del stock (7289,55 €)");
        assertTrue(md.contains("*Informe generado el:"), "Debe incluir la fecha de generación al final");

        // 3. Verificar generación del informe HTML (.html) con CommonMark y tablas GFM
        String html = ConversorXmlMarkdownHtml.convertirMarkdownAHtml(md);
        assertTrue(html.contains("<!DOCTYPE html>"), "Debe ser un documento HTML5 completo");
        assertTrue(html.contains("<table>"), "Debe renderizar la tabla Markdown como etiqueta <table> en HTML");
        assertTrue(html.contains("<td align=\"left\">Portátil Gaming Asus</td>"), "Debe contener las celdas de los productos");
        assertTrue(html.contains("<strong>Total de productos (referencias):</strong> 4"), "Debe incluir el resumen en HTML");
    }

    /**
     * CP-02: Caso límite.
     * Un archivo XML válido pero vacío (sin nodos <producto>) debe generar una lista vacía
     * y un informe con totales a cero (0 referencias, 0 unidades, 0,00 €) sin lanzar excepciones.
     */
    @Test
    @DisplayName("CP-02: Caso límite - Catálogo XML vacío sin nodos <producto>")
    void testCasoLimiteXmlVacio() throws IOException {
        Path xmlVacio = tempDir.resolve("productos_vacio.xml");
        Files.writeString(xmlVacio, """
                <?xml version="1.0" encoding="UTF-8"?>
                <productos>
                </productos>
                """, StandardCharsets.UTF_8);

        List<Producto> productos = ConversorXmlMarkdownHtml.leerProductosDesdeXml(xmlVacio.toFile());
        assertTrue(productos.isEmpty(), "La lista de productos debe estar vacía");

        String md = ConversorXmlMarkdownHtml.generarMarkdown(productos);
        assertTrue(md.contains("**Total de productos (referencias):** 0"));
        assertTrue(md.contains("**Unidades totales en stock:** 0"));
        assertTrue(md.contains("0,00 €") || md.contains("0.00 €"));

        String html = ConversorXmlMarkdownHtml.convertirMarkdownAHtml(md);
        assertTrue(html.contains("<table>"), "La cabecera de la tabla debe seguir generándose en HTML");
    }

    /**
     * CP-03: Fichero inexistente.
     * Comprueba que al intentar leer un fichero que no existe se lanza IllegalArgumentException
     * y que el método main() lo gestiona limpiamente sin colapsar la aplicación.
     */
    @Test
    @DisplayName("CP-03: Fichero inexistente - Control de ruta inválida")
    void testFicheroInexistente() {
        File ficheroNoExiste = tempDir.resolve("no_existe.xml").toFile();

        // Al llamar a leerProductosDesdeXml con un fichero inexistente debe lanzar IllegalArgumentException controlada
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            ConversorXmlMarkdownHtml.leerProductosDesdeXml(ficheroNoExiste);
        });
        assertTrue(ex.getMessage().contains("no se pudo procesar"));

        // Y el método main() debe terminar sin lanzar excepción no controlada
        assertDoesNotThrow(() -> {
            ConversorXmlMarkdownHtml.main(new String[]{ficheroNoExiste.getAbsolutePath()});
        });
    }

    /**
     * CP-04: Entrada incorrecta (Formato numérico inválido).
     * Un XML donde <precio> contiene texto no numérico ("ochocientos") debe lanzar IllegalArgumentException.
     */
    @Test
    @DisplayName("CP-04: Entrada incorrecta - Precio no numérico en el XML")
    void testEntradaIncorrectaPrecioNoNumerico() throws IOException {
        Path xmlErroneo = tempDir.resolve("productos_erroneo.xml");
        Files.writeString(xmlErroneo, """
                <?xml version="1.0" encoding="UTF-8"?>
                <productos>
                    <producto>
                        <id>101</id>
                        <nombre>Portátil Gaming Asus</nombre>
                        <categoria>Informática</categoria>
                        <precio>ochocientos</precio>
                        <cantidad>5</cantidad>
                    </producto>
                </productos>
                """, StandardCharsets.UTF_8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            ConversorXmlMarkdownHtml.leerProductosDesdeXml(xmlErroneo.toFile());
        });

        assertTrue(ex.getMessage().contains("valor numérico válido"),
                "El mensaje de excepción debe indicar que se esperaba un valor numérico válido");
    }

    /**
     * CP-05: Entrada incorrecta (Valores negativos o etiqueta obligatoria ausente).
     * Verifica que no se admitan precios/cantidades negativas ni productos incompletos.
     */
    @Test
    @DisplayName("CP-05: Entrada incorrecta - Cantidad negativa en un producto")
    void testEntradaIncorrectaValoresNegativos() throws IOException {
        Path xmlNegativo = tempDir.resolve("productos_negativo.xml");
        Files.writeString(xmlNegativo, """
                <?xml version="1.0" encoding="UTF-8"?>
                <productos>
                    <producto>
                        <id>105</id>
                        <nombre>Teclado</nombre>
                        <categoria>Periféricos</categoria>
                        <precio>50.00</precio>
                        <cantidad>-3</cantidad>
                    </producto>
                </productos>
                """, StandardCharsets.UTF_8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            ConversorXmlMarkdownHtml.leerProductosDesdeXml(xmlNegativo.toFile());
        });

        assertTrue(ex.getMessage().contains("valores negativos"));
    }
}
