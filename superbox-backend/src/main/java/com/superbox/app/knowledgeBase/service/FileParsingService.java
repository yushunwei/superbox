package com.superbox.app.knowledgeBase.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.superbox.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class FileParsingService {

    public String parse(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new BusinessException(400, "文件名不能为空");

        String ext = filename.toLowerCase();
        try {
            if (ext.endsWith(".pdf")) return parsePdf(file);
            if (ext.endsWith(".docx")) return parseDocx(file);
            if (ext.endsWith(".txt") || ext.endsWith(".md") || ext.endsWith(".markdown")) return parseText(file);
            if (ext.endsWith(".html") || ext.endsWith(".htm")) return parseHtml(file);
            if (ext.endsWith(".csv")) return parseCsv(file);
            throw new BusinessException(400, "不支持的文件格式: " + ext);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("File parsing failed: {}", filename, e);
            throw new BusinessException(500, "文件解析失败: " + e.getMessage());
        }
    }

    private String parsePdf(MultipartFile file) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        try (var pdf = Loader.loadPDF(file.getBytes())) {
            return stripper.getText(pdf);
        }
    }

    private String parseDocx(MultipartFile file) throws IOException {
        try (var doc = new XWPFDocument(file.getInputStream());
             var writer = new StringWriter()) {
            for (var para : doc.getParagraphs()) {
                writer.write(para.getText());
                writer.write('\n');
            }
            return writer.toString();
        }
    }

    private String parseText(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String name = file.getOriginalFilename();
        if (name != null && (name.endsWith(".md") || name.endsWith(".markdown"))) {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(content);
            return TextContentRenderer.builder().build().render(document);
        }
        return content;
    }

    private String parseHtml(MultipartFile file) throws IOException {
        String html = new String(file.getBytes(), StandardCharsets.UTF_8);
        return Jsoup.parse(html).wholeText();
    }

    private String parseCsv(MultipartFile file) throws IOException, CsvValidationException {
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             var writer = new StringWriter()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                writer.write(String.join(" | ", line));
                writer.write('\n');
            }
            return writer.toString();
        }
    }
}
