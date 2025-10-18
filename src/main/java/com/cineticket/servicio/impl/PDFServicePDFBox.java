package com.cineticket.servicio.impl;

import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;
import com.cineticket.servicio.PDFService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PDFServicePDFBox implements PDFService {

    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("es", "CO")); // Tipo de moneda

    private final Path outputDir;

    // ⬅️ Nuevo: permite inyectar carpeta
    public PDFServicePDFBox(String outputDir) {
        try {
            this.outputDir = (outputDir == null || outputDir.isBlank())
                    ? Path.of(System.getProperty("java.io.tmpdir"))
                    : Path.of(outputDir);
            Files.createDirectories(this.outputDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de comprobantes", e);
        }
    }

    // ⬅️ Overload conveniente: por defecto usa tmpdir
    public PDFServicePDFBox() {
        this(System.getProperty("java.io.tmpdir"));
    }

    @Override
    public String generarComprobantePDF(Compra compra,
                                        List<Entrada> entradas,
                                        List<CompraConfiteria> combos,
                                        Map<String, Object> extra) {
        // Ruta temporal por defecto (puedes apuntar a una carpeta “comprobantes/”)
        String fileName = "comprobante_" + (compra.getIdCompra() != null ? compra.getIdCompra() : System.currentTimeMillis()) + ".pdf";
        Path out = Path.of(System.getProperty("java.io.tmpdir"), fileName);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                // Encabezado
                y = title(cs, "CINETICKET", margin, y, 20);
                y = subtitle(cs, "Comprobante de Compra", margin, y, 14);

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                String cliente = str(extra, "clienteNombre", "Cliente N/A");
                String metodo  = (compra.getMetodoPago() != null) ? compra.getMetodoPago().name() : "N/A";

                y = line(cs, "ID: " + n(compra.getIdCompra()) +
                        "   Fecha: " + (compra.getFechaHoraCompra() != null ? compra.getFechaHoraCompra().format(dtf) : "N/A"), margin, y);
                y = line(cs, "Cliente: " + cliente, margin, y);
                y = line(cs, "Método de pago: " + metodo, margin, y);

                y = spacer(cs, y, 10);

                // Entradas
                y = section(cs, "ENTRADAS", margin, y);
                if (entradas != null && !entradas.isEmpty()) {
                    y = tableHeader(cs, new String[]{"Función", "Asiento", "Precio"}, new float[]{200, 120, 100}, margin, y);
                    for (Entrada e : entradas) {
                        String funcionTxt = str(extra, "funcionTexto", "Función " + n(e.getFuncionId()));
                        String asientoTxt = "Asiento " + n(e.getAsientoId());
                        String precioTxt  = money(e.getPrecioUnitario());
                        y = tableRow(cs, new String[]{funcionTxt, asientoTxt, precioTxt}, new float[]{200, 120, 100}, margin, y);
                        if (y < 80) { y = newPage(doc, page = new PDPage(PDRectangle.LETTER), cs); }
                    }
                } else {
                    y = line(cs, "(Sin entradas)", margin, y);
                }

                y = spacer(cs, y, 10);

                // Confitería
                y = section(cs, "CONFITERÍA", margin, y);
                if (combos != null && !combos.isEmpty()) {
                    y = tableHeader(cs, new String[]{"Combo", "Cantidad", "Unitario", "Subtotal"},
                            new float[]{200, 100, 100, 100}, margin, y);
                    for (CompraConfiteria i : combos) {
                        String comboTxt = "Combo " + n(i.getComboId());
                        String cantTxt  = n(i.getCantidad());
                        String unitTxt  = money(i.getPrecioUnitario());
                        String subTxt   = money(i.getSubtotal());
                        y = tableRow(cs, new String[]{comboTxt, cantTxt, unitTxt, subTxt},
                                new float[]{200, 100, 100, 100}, margin, y);
                        if (y < 80) { y = newPage(doc, page = new PDPage(PDRectangle.LETTER), cs); }
                    }
                } else {
                    y = line(cs, "(Sin confitería)", margin, y);
                }

                y = spacer(cs, y, 10);

                // Totales
                y = section(cs, "TOTALES", margin, y);
                y = line(cs, "Total Entradas:   " + money(compra.getTotalEntradas()), margin, y);
                y = line(cs, "Total Confitería: " + money(compra.getTotalConfiteria()), margin, y);
                y = line(cs, "Total General:    " + money(compra.getTotalGeneral() != null
                        ? compra.getTotalGeneral() : compra.getTotalEntradas().add(compra.getTotalConfiteria())), margin, y);

                y = spacer(cs, y, 18);
                small(cs, "Términos: Conserve este comprobante. No se admiten cambios ni devoluciones después del inicio de la función.", margin, y);
            }

            doc.save(out.toFile());
            return out.toAbsolutePath().toString();

        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF de la compra " + n(compra.getIdCompra()), e);
        }
    }

    @Override
    public boolean guardarComprobante(Compra compra, String rutaDestino) {
        if (compra.getRutaComprobantePdf() == null) return false;
        try {
            Path src = Path.of(compra.getRutaComprobantePdf());
            Path dst = Path.of(rutaDestino);
            Files.createDirectories(dst.getParent() != null ? dst.getParent() : dst.toAbsolutePath().getParent());
            Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ===== Helpers de dibujo =====
    private float title(PDPageContentStream cs, String text, float x, float y, int size) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, size); cs.newLineAtOffset(x, y);
        cs.showText(text); cs.endText();
        return y - (size + 6);
    }

    private float subtitle(PDPageContentStream cs, String text, float x, float y, int size) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD, size); cs.newLineAtOffset(x, y);
        cs.showText(text); cs.endText();
        return y - (size + 10);
    }

    private float section(PDPageContentStream cs, String text, float x, float y) throws IOException {
        return subtitle(cs, text, x, y, 12);
    }

    private float line(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, 10); cs.newLineAtOffset(x, y);
        cs.showText(text); cs.endText();
        return y - 14;
    }

    private float small(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8); cs.newLineAtOffset(x, y);
        cs.showText(text); cs.endText();
        return y - 10;
    }

    private float spacer(PDPageContentStream cs, float y, float amount) { return y - amount; }

    private float tableHeader(PDPageContentStream cs, String[] cols, float[] widths, float x, float y) throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        float x0 = x;
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x0, y); cs.showText(cols[i]); cs.endText();
            x0 += widths[i];
        }
        return y - 14;
    }

    private float tableRow(PDPageContentStream cs, String[] cols, float[] widths, float x, float y) throws IOException {
        cs.setFont(PDType1Font.HELVETICA, 10);
        float x0 = x;
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.newLineAtOffset(x0, y); cs.showText(cols[i]); cs.endText();
            x0 += widths[i];
        }
        return y - 14;
    }

    private float newPage(PDDocument doc, PDPage newPage, PDPageContentStream oldCs) throws IOException {
        oldCs.close();
        doc.addPage(newPage);
        return newPage.getMediaBox().getHeight() - 50;
    }

    // ===== Helpers de formato =====
    private String money(java.math.BigDecimal bd) {
        if (bd == null) return moneyFmt.format(0);
        return moneyFmt.format(bd);
    }

    private String str(Map<String, Object> m, String k, String def) {
        if (m == null) return def;
        Object v = m.get(k);
        return v != null ? v.toString() : def;
    }

    private String n(Object o) { return o == null ? "N/A" : String.valueOf(o); }
}
