package messaging.fileUtils.messaging.stats;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.element.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class Test2 {

    // Logging
    final static Logger logger = LogManager.getLogger(Test2.class);


    /**
     * Manipulates a PDF file src with the file dest as result
     *
     * @param src  the original PDF
     * @param dest the resulting PDF
     * @param resizeFactor factor to multiplicate to resize image
     * @throws IOException
     */
    public void manipulatePdf(String src, String dest,Float resizeFactor) throws IOException {

        //Get source pdf
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dest));

        // Iterate over all pages to get all images.
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++)
        {
            PdfPage page = pdfDoc.getPage(i);
            PdfDictionary pageDict = page.getPdfObject();
            PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
            // Get images
            PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
            for (Iterator<PdfName> iter = xObjects.keySet().iterator(); iter.hasNext(); ) {
                // Get image
                PdfName imgRef = iter.next();
                PdfStream stream = xObjects.getAsStream(imgRef);
                PdfImageXObject image = new PdfImageXObject(stream);
                BufferedImage bi = image.getBufferedImage();
                if (bi == null)
                    continue;

                // Create new image
                int width = (int) (bi.getWidth() * resizeFactor);
                int height = (int) (bi.getHeight() * resizeFactor);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                AffineTransform at = AffineTransform.getScaleInstance(resizeFactor, resizeFactor);
                Graphics2D g = img.createGraphics();
                g.drawRenderedImage(bi, at);
                ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();

                // Write new image
                ImageIO.write(img, "JPG", imgBytes);
                Image imgNew =new Image(ImageDataFactory.create(imgBytes.toByteArray()));

                // Replace the original image with the resized image
                xObjects.put(imgRef, imgNew.getXObject().getPdfObject());
            }
        }

        pdfDoc.close();
    }

    /**
     * Main method.
     *
     * @param src  the original PDF
     * @param dest the resulting PDF
     * @param resizeFactor factor to multiplicate to resize image
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        //Get input parametres
        if (args.length < 3 ) {
            System.out.println("Source PDF, Destination PDF and Resize Factor must be provided as parametres");
        } else {
            String sourcePDF=args[0];
            String destPDF=args[1];
            Float resizeFactor=Float.valueOf(new String(args[2]));
            logger.info("Inovking Resize with args, source:" + sourcePDF
                    + " destination:" + destPDF
                    + " factor:" + resizeFactor);
            //Call method to resize images
            new Test2().manipulatePdf(sourcePDF,destPDF,resizeFactor);
            logger.info("PDF resized");
        }
    }

}