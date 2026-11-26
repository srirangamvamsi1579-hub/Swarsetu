package app.swarsetu.college;

import app.swarsetu.common.ApiException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Reads a UPI scanner image and returns the UPI ID it pays (the "pa" value of upi://pay?...). */
public final class QrReader {
    private QrReader() {}

    public static String upiId(byte[] image) {
        BufferedImage img;
        try { img = ImageIO.read(new ByteArrayInputStream(image)); } catch (Exception e) { img = null; }
        if (img == null) throw ApiException.bad("Upload a PNG or JPEG image of the scanner.");
        String text;
        try {
            Result r = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img))),
                Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE, DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)));
            text = r.getText();
        } catch (NotFoundException e) {
            throw ApiException.bad("No QR code was found in the image. Upload a clear, straight photo or screenshot of the scanner.");
        }
        if (text == null || !text.toLowerCase().startsWith("upi://pay")) throw ApiException.bad("This QR code is not a UPI payment code.");
        int q = text.indexOf('?');
        if (q < 0) throw ApiException.bad("This UPI code has no payee.");
        for (String part : text.substring(q + 1).split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equalsIgnoreCase("pa")) {
                return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8).trim().toLowerCase();
            }
        }
        throw ApiException.bad("This UPI code has no payee.");
    }
}
