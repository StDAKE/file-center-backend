package com.huaixv06.fileCenter.utils;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class PdfOcrService {

    private final ITesseract tesseract;

    public PdfOcrService() {
        tesseract = new Tesseract();
        tesseract.setDatapath("D:\\Tesseract\\tessdata"); // 设置tessdata路径
        tesseract.setLanguage("chi_sim"); // 设置语言
    }

    public String extractTextFromPdfRegions(String pdfFilePath, Rectangle region) {
        StringBuilder extractedText = new StringBuilder();

        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage pdfImage = pdfRenderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);

                // 确保区域在图像范围内
                if (region.x + region.width <= pdfImage.getWidth() && region.y + region.height <= pdfImage.getHeight()) {
                    BufferedImage croppedImage = pdfImage.getSubimage(region.x, region.y, region.width, region.height);
                    extractedText.append(performOCR(croppedImage)).append("\n");
                } else {
                    System.err.println("Region out of bounds for page " + pageIndex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error extracting text from PDF: " + e.getMessage();
        } catch (RasterFormatException e) {
            e.printStackTrace();
            return "Error with image format: " + e.getMessage();
        }

        return extractedText.toString();
    }

    private String performOCR(BufferedImage image) {
        String extractedText = "";
        try {
            extractedText = tesseract.doOCR(image);
        } catch (TesseractException e) {
            e.printStackTrace();
        }
        return extractedText;
    }
}