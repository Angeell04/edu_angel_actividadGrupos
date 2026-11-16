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

