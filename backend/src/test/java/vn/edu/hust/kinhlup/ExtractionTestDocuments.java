package vn.edu.hust.kinhlup;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class ExtractionTestDocuments {
    static byte[] pdf(boolean encrypted) throws IOException {
        try (PDDocument document = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            document.getDocumentInformation().setTitle("HUST tuyển sinh");
            document.getDocumentInformation().setAuthor("HUST test author");
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(40, 700);
                content.showText("HUST scholarship information for students.");
                content.endText();
            }
            if (encrypted) {
                document.protect(new StandardProtectionPolicy("owner-password", "reader-password", new AccessPermission()));
            }
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    static byte[] docx() throws IOException {
        try (var document = new XWPFDocument(); var bytes = new ByteArrayOutputStream()) {
            document.getProperties().getCoreProperties().setTitle("Thông báo HUST");
            document.getProperties().getCoreProperties().setCreator("HUST test author");
            document.createParagraph().createRun().setText("Học bổng dành cho sinh viên nghiên cứu khoa học.");
            document.write(bytes);
            return bytes.toByteArray();
        }
    }

    static byte[] pptx() throws IOException {
        try (var presentation = new XMLSlideShow(); var bytes = new ByteArrayOutputStream()) {
            presentation.getProperties().getCoreProperties().setTitle("Hội thảo HUST");
            presentation.createSlide().createTextBox().setText("Sinh viên HUST tham gia nghiên cứu khoa học.");
            presentation.write(bytes);
            return bytes.toByteArray();
        }
    }
}
