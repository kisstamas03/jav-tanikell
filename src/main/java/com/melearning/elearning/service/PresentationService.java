package com.melearning.elearning.service;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Presentation;
import com.melearning.elearning.repository.PresentationRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

@Service
public class PresentationService {

    @Autowired
    private PresentationRepository presentationRepository;

    public List<Presentation> getPresentationsByCourse(Course course) {
        return presentationRepository.findByCourseOrderByOrderIndex(course);
    }

    public Optional<Presentation> getPresentationById(Long id) {
        return presentationRepository.findById(id);
    }

    public Presentation savePresentation(Presentation presentation) {
        return presentationRepository.save(presentation);
    }

    public void deletePresentation(Long id) {
        presentationRepository.deleteById(id);
    }

    // --- ÚJ METÓDUSOK A KONVERTÁLÁSHOZ ---

    public Presentation processAndSavePresentation(MultipartFile file, Course course, Integer orderIndex) throws Exception {
        String fileName = file.getOriginalFilename();
        byte[] fileData = file.getBytes();
        String contentType = file.getContentType();

        if (fileName != null) {
            String lowerName = fileName.toLowerCase();

            // Ha PPT vagy PPTX, akkor konvertálunk
            if (lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                boolean isPptx = lowerName.endsWith(".pptx");
                fileData = convertPptToPdfBytes(fileData, isPptx);

                // Kiterjesztés kicserélése PDF-re
                fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".pdf";
                contentType = "application/pdf";
            }
        }

        Presentation presentation = new Presentation(fileName, fileData, contentType, course, orderIndex);
        return presentationRepository.save(presentation);
    }

    private byte[] convertPptToPdfBytes(byte[] pptBytes, boolean isPptx) throws Exception {
        try (PDDocument pdfDocument = new PDDocument();
             ByteArrayInputStream bais = new ByteArrayInputStream(pptBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Dimension pgsize;
            List<?> slides;

            if (isPptx) {
                XMLSlideShow ppt = new XMLSlideShow(bais);
                pgsize = ppt.getPageSize();
                slides = ppt.getSlides();
            } else {
                HSLFSlideShow ppt = new HSLFSlideShow(bais);
                pgsize = ppt.getPageSize();
                slides = ppt.getSlides();
            }

            // --- FELBONTÁS NÖVELÉSE 3x-osára (Tűéles minőség) ---
            int scale = 3;
            int width = (int) (pgsize.width * scale);
            int height = (int) (pgsize.height * scale);

            for (Object slideObj : slides) {
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();

                // Élsimítás és minőségjavító beállítások bekapcsolása
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                // Fehér háttér rajzolása
                graphics.setPaint(Color.white);
                graphics.fill(new Rectangle2D.Float(0, 0, width, height));

                // A rajzológép felskálázása, hogy kitöltse a nagy vásznat
                graphics.scale(scale, scale);

                // Dia kirajzolása
                if (isPptx) {
                    ((XSLFSlide) slideObj).draw(graphics);
                } else {
                    ((HSLFSlide) slideObj).draw(graphics);
                }

                // PDF oldal létrehozása az EREDETI (kisebb) méretben
                PDPage page = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                pdfDocument.addPage(page);

                // A felskálázott képet JPEG-ként adjuk hozzá (90%-os minőség, hogy ne legyen túl nagy a fájl)
                PDImageXObject pdImage = JPEGFactory.createFromImage(pdfDocument, img, 0.90f);

                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page)) {
                    // A nagy felbontású képet "bepréseljük" az eredeti oldalméretre, ettől lesz éles a monitoron
                    contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                }
            }

            pdfDocument.save(baos);
            return baos.toByteArray();
        }
    }
}