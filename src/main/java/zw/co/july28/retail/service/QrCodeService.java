package zw.co.july28.retail.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zw.co.july28.retail.exception.BadRequestException;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@Slf4j
public class QrCodeService {

    private static final int SIZE = 300;

    /**
     * Generates a QR code PNG for a customer.
     * The encoded value "CUS-{id}" is used by the POS scanner to look up the customer.
     */
    public byte[] generateCustomerQrCode(Long customerId) {
        String content = "CUS-" + customerId;
        return generate(content);
    }

    public byte[] generate(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 2
            );
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("QR code generation failed for content '{}': {}", content, e.getMessage());
            throw new BadRequestException("Failed to generate QR code: " + e.getMessage());
        }
    }
}
