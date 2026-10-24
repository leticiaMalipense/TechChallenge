package br.com.autoshop.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EstimatePDFService {

    public void generatePdf() {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Olá, este é um PDF gerado com Java e PDFBox!");
                contentStream.endText();
            }

            document.save("estimate.pdf");
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
