package com.mathdep.service.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTextExtractionServiceTest {

    private final DocumentTextExtractionService service = new DocumentTextExtractionService();

    @Test
    void extractsTxtAsUtf8() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "notes.txt",
            "text/plain",
            "Definition. A group is a set with an operation.".getBytes(StandardCharsets.UTF_8));

        DocumentTextExtractionService.ExtractedDocument result = service.extract(file);

        assertThat(result.filename()).isEqualTo("notes.txt");
        assertThat(result.extension()).isEqualTo("txt");
        assertThat(result.text()).contains("A group is a set");
        assertThat(result.pages()).hasSize(1);
    }

    @Test
    void extractsMarkdownAsUtf8() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "chapter.md",
            "text/markdown",
            "## Theorem\nEvery finite subgroup has an identity.".getBytes(StandardCharsets.UTF_8));

        DocumentTextExtractionService.ExtractedDocument result = service.extract(file);

        assertThat(result.extension()).isEqualTo("md");
        assertThat(result.text()).contains("Every finite subgroup");
    }

    @Test
    void rejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "spreadsheet.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "not supported".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.extract(file))
            .isInstanceOf(DocumentTextExtractionException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void extractsPdfPageWise() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "continuity.pdf",
            "application/pdf",
            tinyPdf("Continuity theorem text"));

        DocumentTextExtractionService.ExtractedDocument result = service.extract(file);

        assertThat(result.extension()).isEqualTo("pdf");
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.pages()).hasSize(1);
        assertThat(result.text()).contains("[Page 1 of 1]");
        assertThat(result.text()).contains("Continuity theorem text");
    }

    private byte[] tinyPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
