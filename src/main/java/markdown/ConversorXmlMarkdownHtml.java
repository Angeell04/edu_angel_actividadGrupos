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
        // y si no se indica, buscar "productos.xml" en el directorio actual (para ejecución desde .jar/.bat)
        // o en la ruta del código fuente "src/main/java/markdown/productos.xml".
        File xmlFile;
        if (args.length > 0) {
            xmlFile = new File(args[0]);
        } else {
            File localXml = new File("productos.xml");
            xmlFile = localXml.exists() ? localXml : new File("src/main/java/markdown/productos.xml");
        }
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
     * Incluye:
     * <ul>
     *   <li>Tabla formateada con alineación para textos e importes numéricos.</li>
     *   <li>Sección de Resumen con total de referencias, unidades totales y valor económico total del stock.</li>
     *   <li>Pie de documento con la fecha y hora exacta de generación.</li>
     * </ul>
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

