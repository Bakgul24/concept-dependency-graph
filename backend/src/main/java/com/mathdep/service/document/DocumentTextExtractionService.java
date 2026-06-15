package com.mathdep.service.document;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTextExtractionService {

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentTextExtractionException(HttpStatus.BAD_REQUEST, "Uploaded file is empty.");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);

        try {
            return switch (extension) {
                case "txt", "md" -> extractUtf8Text(file, filename, extension);
                case "pdf" -> extractPdf(file, filename);
                default -> throw new DocumentTextExtractionException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported file type '.%s'. Supported file types are .txt, .md, and .pdf.".formatted(extension));
            };
        } catch (IOException e) {
            throw new DocumentTextExtractionException(
                HttpStatus.BAD_REQUEST,
                "Could not read uploaded file '%s'.".formatted(filename),
                e);
        }
    }

    private ExtractedDocument extractUtf8Text(
            MultipartFile file,
            String filename,
            String extension) throws IOException {

        String text = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        if (text.isBlank()) {
            throw new DocumentTextExtractionException(
                HttpStatus.BAD_REQUEST,
                "File '%s' has no extractable text.".formatted(filename));
        }

        return ExtractedDocument.builder()
            .filename(filename)
            .extension(extension)
            .totalPages(null)
            .pages(List.of(PageText.builder()
                .pageNumber(1)
                .text(text)
                .build()))
            .text(text)
            .build();
    }

    private ExtractedDocument extractPdf(MultipartFile file, String filename) throws IOException {
        try (PDDocument document = PDDocument.load(file.getBytes())) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            StringBuilder allText = new StringBuilder();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                pages.add(PageText.builder()
                    .pageNumber(page)
                    .text(pageText)
                    .build());

                if (!pageText.isBlank()) {
                    if (allText.length() > 0) {
                        allText.append("\n\n");
                    }
                    allText.append("[Page ").append(page).append(" of ").append(totalPages).append("]\n")
                        .append(pageText);
                }
            }

            String text = allText.toString().trim();
            if (text.isBlank()) {
                throw new DocumentTextExtractionException(
                    HttpStatus.BAD_REQUEST,
                    "PDF '%s' has no extractable text. It may be scanned or image-only.".formatted(filename));
            }

            log.info("Extracted {} chars from PDF '{}' across {} pages", text.length(), filename, totalPages);
            return ExtractedDocument.builder()
                .filename(filename)
                .extension("pdf")
                .totalPages(totalPages)
                .pages(pages)
                .text(text)
                .build();
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-file";
        }

        String normalized = originalFilename.replace("\\", "/");
        return normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }

        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Builder
    public record ExtractedDocument(
        String filename,
        String extension,
        Integer totalPages,
        List<PageText> pages,
        String text
    ) {
    }

    @Builder
    public record PageText(
        int pageNumber,
        String text
    ) {
    }
}
