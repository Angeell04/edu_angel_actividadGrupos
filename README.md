# Memoria de Práctica — Ejercicio 6: Conversión de XML a Markdown (`.md`) y HTML (`.html`)

---

## 1. Instrucciones de ejecución

### 1.1. Finalidad de la aplicación
La aplicación **`ConversorXmlMarkdownHtml`** tiene como finalidad automatizar la lectura de un catálogo de productos almacenado en un fichero estructurado XML (`productos.xml`) y transformarlo en dos informes legibles y listos para su distribución:
1. **Informe en formato Markdown (`productos.md`)**: presenta los productos en una tabla alineada con cálculo de subtotales por línea, un resumen ejecutivo del inventario (total de referencias, total de unidades y valor económico total del stock: `precio × cantidad`) y la marca temporal de generación.
2. **Informe en formato HTML5 (`productos.html`)**: generado directamente a partir del contenido del fichero `.md` utilizando la librería **`commonmark-java`** junto con su extensión para tablas GFM (*GitHub Flavored Markdown*), aplicando una hoja de estilos CSS integrada para su visualización directa en cualquier navegador web.

### 1.2. Requisitos y librerías
* **Entorno de ejecución**: Java Development Kit (**JDK 15 o superior**, probado en OpenJDK 26).
* **Gestor de dependencias y construcción**: **Apache Maven** (el proyecto incluye el wrapper `mvnw` / `mvnw.cmd`).
* **Librerías empleadas en `pom.xml`**:
  * **API estándar de Java XML DOM (`javax.xml.parsers` / `org.w3c.dom`)**: incluida en el JDK para el parseo jerárquico del archivo `productos.xml`.
  * **Java NIO.2 (`java.nio.file`) y `java.time`**: incluidas en el JDK para la lectura/escritura eficiente en codificación `UTF-8` y el formateo de fechas (`LocalDateTime`, `DateTimeFormatter`).
  * **CommonMark (`org.commonmark:commonmark:0.24.0`)**: librería externa para parsear el documento Markdown en un árbol sintáctico (AST) y renderizarlo a HTML.
  * **CommonMark GFM Tables (`org.commonmark:commonmark-ext-gfm-tables:0.24.0`)**: extensión oficial necesaria para que el parser y el renderizador de CommonMark reconozcan la sintaxis de tablas (`| col |`) y las conviertan en etiquetas `<table>`, `<thead>`, `<tbody>`, `<tr>`, `<th>` y `<td>`.
  * **JUnit 5 Jupiter (`org.junit.jupiter:junit-jupiter-api:5.13.2` y `junit-jupiter-engine:5.13.2`)**: framework de pruebas unitarias para la ejecución automatizada de la batería de pruebas.

```xml
<!-- Dependencias en pom.xml para conversión Markdown -> HTML y pruebas JUnit Jupiter -->
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>0.24.0</version>
</dependency>
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark-ext-gfm-tables</artifactId>
    <version>0.24.0</version>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.13.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.13.2</version>
    <scope>test</scope>
</dependency>
```

### 1.3. Ficheros de entrada
* **Ruta por defecto**: `src/main/java/markdown/productos.xml` (también admite recibir otra ruta como argumento de línea de comandos `args[0]`).
* **Estructura esperada**: Documento XML codificado en `UTF-8` con un elemento raíz `<productos>` que contiene cero, uno o varios nodos `<producto>`, cada uno con las etiquetas `<id>`, `<nombre>`, `<categoria>`, `<precio>` (decimal no negativo) y `<cantidad>` (entero no negativo).

### 1.4. Pasos para ejecutar la aplicación y las pruebas JUnit
**Opción A — Desde el IDE (IntelliJ IDEA):**
1. Abrir el proyecto Maven `actividad1ACD` y sincronizar `pom.xml`.
2. Para ejecutar la aplicación: abrir `src/main/java/markdown/ConversorXmlMarkdownHtml.java`, clic derecho y seleccionar **Run 'ConversorXmlMarkdownHtml.main()'**.
3. Para ejecutar los tests JUnit Jupiter: abrir `src/test/java/markdown/ConversorXmlMarkdownHtmlTest.java`, clic derecho y seleccionar **Run 'ConversorXmlMarkdownHtmlTest'**.

**Opción B — Desde la terminal con Maven Wrapper:**
1. Situarse en la raíz del proyecto (`C:\Users\miran\IdeaProjects\actividad1ACD`).
2. Ejecutar la aplicación principal:
   ```powershell
   .\mvnw.cmd compile exec:java "-Dexec.mainClass=markdown.ConversorXmlMarkdownHtml"
   ```
3. Ejecutar la suite de pruebas **JUnit Jupiter**:
   ```powershell
   .\mvnw.cmd test -Dtest=ConversorXmlMarkdownHtmlTest
   ```

### 1.5. Resultados que genera
1. **Salida por consola**: Trazabilidad de la lectura del árbol DOM (elemento raíz, número de nodos `<producto>` encontrados y desglose de atributos de cada producto leído) junto con las rutas absolutas de los ficheros generados.
2. **Fichero `productos.md`**: Informe en formato Markdown ubicado en la raíz del proyecto con la tabla de productos, el bloque de resumen y la fecha de generación.
3. **Fichero `productos.html`**: Documento HTML5 completo y estilizado en la raíz del proyecto, generado a partir de `productos.md`.

---

## 2. Código documentado

A continuación se incluye el código fuente documentado con comentarios Javadoc y explicaciones de las decisiones técnicas de las clases principales y de la clase de pruebas JUnit Jupiter.

### 2.1. Clase modelo (`src/main/java/markdown/Producto.java`)

```java
package markdown;

/**
 * Clase modelo (POJO) que representa un producto del catálogo.
 * Almacena los datos leídos de cada nodo {@code <producto>} del archivo XML
 * y proporciona un método para calcular el subtotal de su stock.
 */
public class Producto {
    private String id;
    private String nombre;
    private String categoria;
    private double precio;
    private int cantidad;

    /**
     * Constructor para inicializar un producto con todos sus atributos.
     *
     * @param id        Identificador único del producto.
     * @param nombre    Nombre descriptivo del producto.
     * @param categoria Categoría a la que pertenece el producto.
     * @param precio    Precio unitario en euros.
     * @param cantidad  Unidades disponibles en stock.
     */
    public Producto(String id, String nombre, String categoria, double precio, int cantidad) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    /**
     * Calcula el valor económico total del stock de este producto (precio × cantidad).
     *
     * @return El subtotal del stock en euros.
     */
    public double getValorStock() {
        return precio * cantidad;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public double getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }
}
```

### 2.2. Clase principal (`src/main/java/markdown/ConversorXmlMarkdownHtml.java`)

```java
package markdown;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aplicación encargada de leer un catálogo de productos desde un fichero XML ({@code productos.xml}),
 * generar un informe estructurado en formato Markdown ({@code productos.md}) con resumen de inventario
 * y fecha de generación, y finalmente convertir dicho fichero Markdown a una página HTML ({@code productos.html})
 * empleando la librería {@code commonmark-java} con extensión de tablas GFM.
 */
public class ConversorXmlMarkdownHtml {

    /**
     * Método principal de ejecución.
     * Permite recibir opcionalmente por argumentos la ruta del XML de entrada,
     * o utiliza por defecto {@code src/main/java/markdown/productos.xml}.
     *
     * @param args Argumentos de línea de comandos (opcional: ruta del fichero XML).
     */
    public static void main(String[] args) {
        // Decisión de diseño: Permitir ruta por parámetro para facilitar las pruebas (caso límite / fichero inexistente)
        // manteniendo por defecto la ruta original de la práctica.
        String rutaXml = (args.length > 0) ? args[0] : "src/main/java/markdown/productos.xml";
        File xmlFile = new File(rutaXml);
        Path mdPath = Path.of("productos.md");
        Path htmlPath = Path.of("productos.html");

        // Validación previa de la existencia del fichero de entrada
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            System.err.println("[ERROR] El fichero XML de entrada no existe o no es válido: " + xmlFile.getAbsolutePath());
            return;
        }

        try {
            // 1. Leer productos del XML mediante el parser DOM
            List<Producto> productos = leerProductosDesdeXml(xmlFile);

            // 2. Generar archivo Markdown (.md) con tabla, resumen y fecha (Apartado 6.a)
            String markdownContent = generarMarkdown(productos);
            Files.writeString(mdPath, markdownContent, StandardCharsets.UTF_8);
            System.out.println("\n[OK] Archivo Markdown generado: " + mdPath.toAbsolutePath());

            // 3. Generar versión HTML (.html) a partir del fichero .md usando CommonMark (Apartado 6.b)
            String mdLeido = Files.readString(mdPath, StandardCharsets.UTF_8);
            String htmlContent = convertirMarkdownAHtml(mdLeido);
            Files.writeString(htmlPath, htmlContent, StandardCharsets.UTF_8);
            System.out.println("[OK] Archivo HTML generado:     " + htmlPath.toAbsolutePath());

        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR DE DATOS] Formato inválido en el fichero XML: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[ERROR E/S] Problema al leer o escribir los ficheros: " + e.getMessage());
        }
    }

    /**
     * Parsea el archivo XML utilizando la API estándar DOM ({@link DocumentBuilder})
     * y transforma cada nodo {@code <producto>} en un objeto {@link Producto}.
     * <p>
     * Se ha elegido DOM porque el catálogo de productos es de tamaño reducido/medio y
     * permite recorrer y validar fácilmente la estructura jerárquica en memoria.
     * </p>
     *
     * @param file Fichero XML de entrada.
     * @return Lista de objetos {@link Producto} extraídos del XML.
     */
    public static List<Producto> leerProductosDesdeXml(File file) {
        List<Producto> productos = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            // Normaliza el árbol DOM para evitar nodos de texto fragmentados o vacíos
            document.getDocumentElement().normalize();

            System.out.println("Elemento raiz: " + document.getDocumentElement().getNodeName());

            NodeList listaProductos = document.getElementsByTagName("producto");
            System.out.println("Nodos producto a recorrer: " + listaProductos.getLength());

            for (int i = 0; i < listaProductos.getLength(); i++) {
                System.out.println("========= Producto " + (i + 1) + " =========");
                org.w3c.dom.Node prod = listaProductos.item(i);

                if (prod.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element element = (Element) prod;

                    String id = obtenerTextoElemento(element, "id");
                    String nombre = obtenerTextoElemento(element, "nombre");
                    String categoria = obtenerTextoElemento(element, "categoria");
                    double precio = Double.parseDouble(obtenerTextoElemento(element, "precio").replace(",", "."));
                    int cantidad = Integer.parseInt(obtenerTextoElemento(element, "cantidad"));

                    // Validación de valores negativos como caso límite / entrada incorrecta
                    if (precio < 0 || cantidad < 0) {
                        throw new IllegalArgumentException(
                                "El producto con ID " + id + " contiene valores negativos (precio o cantidad).");
                    }

                    System.out.println("ID: " + id);
                    System.out.println("Nombre: " + nombre);
                    System.out.println("Categoría: " + categoria);
                    System.out.println("Precio: " + precio);
                    System.out.println("Cantidad: " + cantidad);

                    productos.add(new Producto(id, nombre, categoria, precio, cantidad));
                }
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un valor numérico válido en <precio> o <cantidad>.", e);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("El archivo XML está mal formado o no se pudo procesar: " + e.getMessage(), e);
        }

        return productos;
    }

    /**
     * Método auxiliar seguro para obtener el contenido de texto de una etiqueta hija dentro de un elemento XML.
     * Evita {@link NullPointerException} si falta alguna etiqueta obligatoria en el XML.
     *
     * @param parent  Elemento padre ({@code <producto>}).
     * @param tagName Nombre de la etiqueta hija a buscar.
     * @return Texto contenido en la etiqueta, sin espacios iniciales ni finales.
     */
    private static String obtenerTextoElemento(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0 || list.item(0) == null) {
            throw new IllegalArgumentException("Falta la etiqueta obligatoria <" + tagName + "> en un producto.");
        }
        return list.item(0).getTextContent().trim();
    }

    /**
     * Construye el contenido del informe en formato Markdown (.md) a partir de la lista de productos.
     *
     * @param productos Lista de productos del catálogo.
     * @return Cadena con el documento Markdown completo.
     */
    public static String generarMarkdown(List<Producto> productos) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Catálogo de Productos\n\n");
        sb.append("| ID | Nombre | Categoría | Precio (€) | Cantidad | Subtotal (€) |\n");
        sb.append("|:---|:-------|:----------|-----------:|---------:|-------------:|\n");

        double valorTotalStock = 0.0;
        int unidadesTotales = 0;

        for (Producto p : productos) {
            double subtotal = p.getValorStock();
            valorTotalStock += subtotal;
            unidadesTotales += p.getCantidad();

            sb.append(String.format("| %s | %s | %s | %.2f | %d | %.2f |\n",
                    p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getCantidad(), subtotal));
        }

        sb.append("\n## Resumen\n\n");
        sb.append(String.format("- **Total de productos (referencias):** %d\n", productos.size()));
        sb.append(String.format("- **Unidades totales en stock:** %d\n", unidadesTotales));
        sb.append(String.format("- **Valor total del stock:** %.2f €\n", valorTotalStock));

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        sb.append("\n---\n");
        sb.append(String.format("*Informe generado el: %s*\n", fecha));

        return sb.toString();
    }

    /**
     * Convierte el contenido en formato Markdown a un documento HTML5 completo y estilizado.
     * <p>
     * Decisión técnica: Se emplea la librería {@code org.commonmark} junto con la extensión
     * {@code commonmark-ext-gfm-tables} ({@link TablesExtension}), ya que el estándar básico de CommonMark
     * no incluye tablas por defecto y son necesarias para renderizar correctamente la tabla de productos en HTML.
     * </p>
     *
     * @param markdownContent Contenido del fichero Markdown ({@code .md}).
     * @return Documento HTML completo con estilos CSS integrados.
     */
    public static String convertirMarkdownAHtml(String markdownContent) {
        // Activamos la extensión de tablas GFM (GitHub Flavored Markdown)
        List<Extension> extensions = Collections.singletonList(TablesExtension.create());

        Parser parser = Parser.builder()
                .extensions(extensions)
                .build();

        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();

        // Parseamos el documento Markdown a un árbol de nodos AST y lo renderizamos a cuerpo HTML
        Node document = parser.parse(markdownContent);
        String bodyHtml = renderer.render(document);

        // Envolvemos el fragmento HTML generado en una plantilla HTML5 con estilos CSS limpios
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <title>Catálogo de Productos</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px auto; max-width: 900px; color: #333; line-height: 1.6; }
                        h1, h2 { color: #1a365d; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }
                        table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                        th, td { border: 1px solid #cbd5e1; padding: 10px 14px; }
                        th { background-color: #f1f5f9; color: #0f172a; font-weight: 600; }
                        tr:nth-child(even) { background-color: #f8fafc; }
                        hr { border: 0; border-top: 1px solid #cbd5e1; margin: 24px 0; }
                        em { color: #64748b; font-size: 0.9em; }
                    </style>
                </head>
                <body>
                """ + bodyHtml + """
                </body>
                </html>
                """;
    }
}
```

### 2.3. Clase de pruebas JUnit Jupiter (`src/test/java/markdown/ConversorXmlMarkdownHtmlTest.java`)

```java
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

    @Test
    @DisplayName("CP-03: Fichero inexistente - Control de ruta inválida")
    void testFicheroInexistente() {
        File ficheroNoExiste = tempDir.resolve("no_existe.xml").toFile();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            ConversorXmlMarkdownHtml.leerProductosDesdeXml(ficheroNoExiste);
        });
        assertTrue(ex.getMessage().contains("no se pudo procesar"));

        assertDoesNotThrow(() -> {
            ConversorXmlMarkdownHtml.main(new String[]{ficheroNoExiste.getAbsolutePath()});
        });
    }

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

        assertTrue(ex.getMessage().contains("valor numérico válido"));
    }

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
```

---

## 3. Tabla de pruebas (JUnit Jupiter)

Se han implementado y ejecutado automatizadamente con **JUnit 5 (Jupiter)** cinco casos de prueba en `ConversorXmlMarkdownHtmlTest`:

| ID Caso | Método JUnit (`@Test`) | Tipo de prueba | Entrada utilizada | Resultado esperado | Resultado obtenido | Valoración |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **CP-01** | `testFuncionamientoNormal()` | **Funcionamiento normal** | `src/main/java/markdown/productos.xml` (4 productos válidos, IDs 101 a 104). | `leerProductosDesdeXml` devuelve 4 objetos `Producto`; `generarMarkdown` incluye `4` referencias, `45` unidades, `7289,55 €` y fecha; `convertirMarkdownAHtml` genera `<table>` y HTML5 completo. | Todas las aserciones (`assertEquals`, `assertTrue`) pasan correctamente. | **SUPERADO (PASS)** |
| **CP-02** | `testCasoLimiteXmlVacio()` | **Caso límite** (XML vacío sin productos) | `productos_vacio.xml` (`<productos></productos>` creado en `@TempDir`). | Devuelve lista vacía (`isEmpty() == true`); el Markdown y HTML generados muestran la cabecera de tabla y resumen en `0` referencias, `0` unidades y `0,00 €`. | Todas las aserciones pasan sin lanzar excepciones. | **SUPERADO (PASS)** |
| **CP-03** | `testFicheroInexistente()` | **Fichero inexistente** | Ruta inexistente `no_existe.xml` en `@TempDir`. | `leerProductosDesdeXml` lanza `IllegalArgumentException` controlada y `main()` muestra `[ERROR] El fichero XML de entrada no existe...` sin colapsar (`assertDoesNotThrow`). | `assertThrows` y `assertDoesNotThrow` verificados con éxito. | **SUPERADO (PASS)** |
| **CP-04** | `testEntradaIncorrectaPrecioNoNumerico()` | **Entrada incorrecta** (Formato numérico inválido) | `productos_erroneo.xml` con `<precio>ochocientos</precio>`. | Captura `NumberFormatException` y lanza `IllegalArgumentException` indicando que se esperaba un valor numérico válido. | `assertThrows(IllegalArgumentException.class)` superado con el mensaje esperado. | **SUPERADO (PASS)** |
| **CP-05** | `testEntradaIncorrectaValoresNegativos()` | **Entrada incorrecta** (Stock negativo) | `productos_negativo.xml` con `<cantidad>-3</cantidad>`. | Detecta el valor negativo y lanza `IllegalArgumentException` indicando que contiene valores negativos. | `assertThrows(IllegalArgumentException.class)` superado con éxito. | **SUPERADO (PASS)** |

---

## 4. Evidencias y registro de errores corregidos

### 4.1. Detección y corrección de errores durante el desarrollo
Durante la revisión y prueba de la versión inicial de `ConversorXmlMarkdownHtml.java`, se detectaron y corrigieron los siguientes errores:

1. **Falta de generación del archivo HTML (Apartado 6.b)**:
   * **Problema detectado**: Aunque las dependencias `commonmark` y `commonmark-ext-gfm-tables` estaban declaradas en `pom.xml`, el método `main` terminaba tras escribir `productos.md` y no generaba el archivo `productos.html`.
   * **Corrección aplicada**: Se implementó el método `convertirMarkdownAHtml(String markdownContent)` configurando `Parser` y `HtmlRenderer` con `TablesExtension.create()`, envolviendo la salida en una estructura HTML5 con estilos CSS y guardando el resultado en `productos.html`.
2. **Visibilidad de métodos para pruebas unitarias con JUnit**:
   * **Problema detectado**: Los métodos `leerProductosDesdeXml` y `generarMarkdown` tenían visibilidad `private`, lo que impedía probarlos unitariamente desde `ConversorXmlMarkdownHtmlTest`.
   * **Corrección aplicada**: Se cambió su visibilidad a `public static`, permitiendo su verificación directa desde los tests de JUnit Jupiter.
3. **Vulnerabilidad ante ficheros inexistentes, datos negativos o etiquetas ausentes**:
   * **Problema detectado**: En la versión inicial cualquier excepción lanzaba un `RuntimeException(e)` genérico y no se validaban precios/cantidades negativas ni etiquetas faltantes.
   * **Corrección aplicada**: Se añadió validación previa de existencia del fichero, el método auxiliar `obtenerTextoElemento(Element, String)` y control de valores negativos, verificando todos los casos con `assertThrows` en JUnit 5.

---

### 4.2. Evidencia 1: Ejecución de la suite de pruebas JUnit Jupiter (`mvnw test`)

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running markdown.ConversorXmlMarkdownHtmlTest
[ERROR] El fichero XML de entrada no existe o no es válido: C:\Users\miran\AppData\Local\Temp\junit-601617319052265669\no_existe.xml
Elemento raiz: productos
Nodos producto a recorrer: 0
Elemento raiz: productos
Nodos producto a recorrer: 1
========= Producto 1 =========
Elemento raiz: productos
Nodos producto a recorrer: 1
========= Producto 1 =========
Elemento raiz: productos
Nodos producto a recorrer: 4
========= Producto 1 =========
ID: 101
Nombre: Portátil Gaming Asus
Categoría: Informática
Precio: 899.99
Cantidad: 5
========= Producto 2 =========
ID: 102
Nombre: Ratón Inalámbrico Logitech
Categoría: Periféricos
Precio: 24.5
Cantidad: 20
========= Producto 3 =========
ID: 103
Nombre: Teclado Mecánico RGB
Categoría: Periféricos
Precio: 65.0
Cantidad: 12
========= Producto 4 =========
ID: 104
Nombre: Monitor 27 Pulgadas 144Hz
Categoría: Monitores
Precio: 189.95
Cantidad: 8
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.178 s -- in markdown.ConversorXmlMarkdownHtmlTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

### 4.3. Evidencia 2: Fichero de entrada (`src/main/java/markdown/productos.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<productos>
    <producto>
        <id>101</id>
        <nombre>Portátil Gaming Asus</nombre>
        <categoria>Informática</categoria>
        <precio>899.99</precio>
        <cantidad>5</cantidad>
    </producto>
    <producto>
        <id>102</id>
        <nombre>Ratón Inalámbrico Logitech</nombre>
        <categoria>Periféricos</categoria>
        <precio>24.50</precio>
        <cantidad>20</cantidad>
    </producto>
    <producto>
        <id>103</id>
        <nombre>Teclado Mecánico RGB</nombre>
        <categoria>Periféricos</categoria>
        <precio>65.00</precio>
        <cantidad>12</cantidad>
    </producto>
    <producto>
        <id>104</id>
        <nombre>Monitor 27 Pulgadas 144Hz</nombre>
        <categoria>Monitores</categoria>
        <precio>189.95</precio>
        <cantidad>8</cantidad>
    </producto>
</productos>
```

---

### 4.4. Evidencia 3: Fichero Markdown generado (`productos.md`)

```markdown
# Catálogo de Productos

| ID | Nombre | Categoría | Precio (€) | Cantidad | Subtotal (€) |
|:---|:-------|:----------|-----------:|---------:|-------------:|
| 101 | Portátil Gaming Asus | Informática | 899,99 | 5 | 4499,95 |
| 102 | Ratón Inalámbrico Logitech | Periféricos | 24,50 | 20 | 490,00 |
| 103 | Teclado Mecánico RGB | Periféricos | 65,00 | 12 | 780,00 |
| 104 | Monitor 27 Pulgadas 144Hz | Monitores | 189,95 | 8 | 1519,60 |

## Resumen

- **Total de productos (referencias):** 4
- **Unidades totales en stock:** 45
- **Valor total del stock:** 7289,55 €

---
*Informe generado el: 16/11/2026 10:42:14*
```

---

### 4.5. Evidencia 4: Fichero HTML generado (`productos.html`)

```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Catálogo de Productos</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px auto; max-width: 900px; color: #333; line-height: 1.6; }
        h1, h2 { color: #1a365d; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
        th, td { border: 1px solid #cbd5e1; padding: 10px 14px; }
        th { background-color: #f1f5f9; color: #0f172a; font-weight: 600; }
        tr:nth-child(even) { background-color: #f8fafc; }
        hr { border: 0; border-top: 1px solid #cbd5e1; margin: 24px 0; }
        em { color: #64748b; font-size: 0.9em; }
    </style>
</head>
<body>
<h1>Catálogo de Productos</h1>
<table>
<thead>
<tr>
<th align="left">ID</th>
<th align="left">Nombre</th>
<th align="left">Categoría</th>
<th align="right">Precio (€)</th>
<th align="right">Cantidad</th>
<th align="right">Subtotal (€)</th>
</tr>
</thead>
<tbody>
<tr>
<td align="left">101</td>
<td align="left">Portátil Gaming Asus</td>
<td align="left">Informática</td>
<td align="right">899,99</td>
<td align="right">5</td>
<td align="right">4499,95</td>
</tr>
<tr>
<td align="left">102</td>
<td align="left">Ratón Inalámbrico Logitech</td>
<td align="left">Periféricos</td>
<td align="right">24,50</td>
<td align="right">20</td>
<td align="right">490,00</td>
</tr>
<tr>
<td align="left">103</td>
<td align="left">Teclado Mecánico RGB</td>
<td align="left">Periféricos</td>
<td align="right">65,00</td>
<td align="right">12</td>
<td align="right">780,00</td>
</tr>
<tr>
<td align="left">104</td>
<td align="left">Monitor 27 Pulgadas 144Hz</td>
<td align="left">Monitores</td>
<td align="right">189,95</td>
<td align="right">8</td>
<td align="right">1519,60</td>
</tr>
</tbody>
</table>
<h2>Resumen</h2>
<ul>
<li><strong>Total de productos (referencias):</strong> 4</li>
<li><strong>Unidades totales en stock:</strong> 45</li>
<li><strong>Valor total del stock:</strong> 7289,55 €</li>
</ul>
<hr />
<p><em>Informe generado el: 16/11/2026 10:42:14</em></p>
</body>
</html>
```
