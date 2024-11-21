package com.huaixv06.fileCenter.utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class OCRService {

    public String extractTextFromPDF(String pdfFilePath) {
        try {
            // Convert PDF to images
            List<BufferedImage> images = convertPDFToImages(pdfFilePath);

            // Use OCR to extract text from images
            StringBuilder extractedText = new StringBuilder();
            for (BufferedImage image : images) {
                extractedText.append(performOCR(image)).append("\n");
            }

            return extractedText.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error extracting text from PDF.";
        }
    }

    private List<BufferedImage> convertPDFToImages(String pdfFilePath) throws Exception {
        List<BufferedImage> images = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // 遍历每一页并渲染为图像
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300); // 使用300 DPI渲染图像
                images.add(image);
            }
        }

        return images;
    }

    private String performOCR(BufferedImage image) throws Exception {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("D:\\Tesseract\\tessdata"); // 设置tessdata路径
        tesseract.setLanguage("chi_sim"); // 设置语言
        return tesseract.doOCR(image);
    }
}
