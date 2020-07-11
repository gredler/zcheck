package net.gredler.zcheck;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Reader;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.maxicode.MaxiCodeReader;
import com.google.zxing.oned.CodaBarReader;
import com.google.zxing.oned.Code128Reader;
import com.google.zxing.oned.Code39Reader;
import com.google.zxing.oned.Code93Reader;
import com.google.zxing.oned.rss.RSS14Reader;
import com.google.zxing.oned.rss.expanded.RSSExpandedReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

import uk.org.okapibarcode.backend.AztecCode;
import uk.org.okapibarcode.backend.Codabar;
import uk.org.okapibarcode.backend.Code128;
import uk.org.okapibarcode.backend.Code3Of9;
import uk.org.okapibarcode.backend.Code3Of9Extended;
import uk.org.okapibarcode.backend.Code93;
import uk.org.okapibarcode.backend.DataBar14;
import uk.org.okapibarcode.backend.DataBarExpanded;
import uk.org.okapibarcode.backend.DataMatrix;
import uk.org.okapibarcode.backend.MaxiCode;
import uk.org.okapibarcode.backend.Pdf417;
import uk.org.okapibarcode.backend.QrCode;

class ZXing {

    public static DecodeResult decode(BufferedImage image, String originalData, Map< String, Object > okapiConfig) {
        try {

            if (image == null) {
                throw new IllegalArgumentException("Decoding skipped");
            }

            var source = new BufferedImageLuminanceSource(image);
            var bitmap = new BinaryBitmap(new HybridBinarizer(source));

            var hints = new HashMap< DecodeHintType, Boolean >();
            hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            var reader = findReader(okapiConfig);
            var result = reader.decode(bitmap, hints);
            var decodedDataMassaged = massageDecodedData(result.getText(), okapiConfig);
            var originalDataMassaged = massageOriginalData(originalData, okapiConfig);
            return new DecodeResult(originalDataMassaged, decodedDataMassaged, null);

        } catch (Exception e) {
            return new DecodeResult(originalData, null, e);
        }
    }

    private static Reader findReader(Map< String, Object > okapiConfig) {
        var clazz = okapiConfig.get("okapi_class");
        var mode = okapiConfig.get("okapi_mode");
        if (AztecCode.class.getName().equals(clazz)) {
            return new AztecReader();
        } else if (Codabar.class.getName().equals(clazz)) {
            return new CodaBarReader();
        } else if (Code3Of9.class.getName().equals(clazz)) {
            return new Code39Reader();
        } else if (Code3Of9Extended.class.getName().equals(clazz)) {
            return new Code39Reader(false, true);
        } else if (Code93.class.getName().equals(clazz)) {
            return new Code93Reader();
        } else if (Code128.class.getName().equals(clazz)) {
            return new Code128Reader();
        } else if (DataBar14.class.getName().equals(clazz)) {
            return new RSS14Reader();
        } else if (DataBarExpanded.class.getName().equals(clazz)) {
            return new RSSExpandedReader();
        } else if (DataMatrix.class.getName().equals(clazz)) {
            return new DataMatrixReader();
        } else if (MaxiCode.class.getName().equals(clazz)) {
            return new MaxiCodeReader();
        } else if (Pdf417.class.getName().equals(clazz) && !"MICRO".equals(mode)) {
            return new PDF417Reader();
        } else if (QrCode.class.getName().equals(clazz)) {
            return new QRCodeReader();
        } else {
            throw new IllegalStateException("No reader found");
        }
    }

    private static String massageOriginalData(String data, Map< String, Object > okapiConfig) {
        var clazz = okapiConfig.get("okapi_class");
        if (Codabar.class.getName().equals(clazz)) {
            // remove the start/stop characters from the specified barcode content
            return data.substring(1, data.length() - 1);
        } else if (MaxiCode.class.getName().equals(clazz)) {
            // combine primary message and secondary message
            var primary = okapiConfig.get("okapi_primary");
            if (primary != null) {
                var mode = (Integer) okapiConfig.get("okapi_mode");
                var p = primary.toString();
                int postalCodeLength = (mode == 2 ? 9 : 6);
                return p.substring(0, postalCodeLength) + '\u001D' +
                       p.substring(9, 12) + '\u001D' +
                       p.substring(12) + '\u001D' +
                       data;
            }
        } else if (DataBarExpanded.class.getName().equals(clazz)) {
            // some GS1 AI data fields contain check digits; we need to ensure that check digits
            // for AIs 00, 01 and 02 are correct, since ZXing will correct them during decoding
            // https://sourceforge.net/p/zint/mailman/message/37057530/
            var pattern = Pattern.compile("\\[0[012]\\]\\d+");
            var matcher = pattern.matcher(data);
            if (matcher.find()) {
                return data.substring(0, matcher.start()) +
                       tweakGs1CheckDigit(matcher.group()) +
                       data.substring(matcher.end());
            }
        }
        // no massaging required
        return data;
    }

    private static String tweakGs1CheckDigit(String s) {
        // expected input: "[0X]NNNNNNN...D", where X = 0/1/2, N = number, D = check digit
        int sum = 0;
        boolean three = true;
        int end = s.lastIndexOf(']');
        for (int i = s.length() - 2; i > end; i--) {
            sum += (s.charAt(i) - '0') * (three ? 3 : 1);
            three = !three;
        }
        int checkDigit = 10 - (sum % 10);
        if (checkDigit == 10) {
            checkDigit = 0;
        }
        return s.substring(0, s.length() - 1) + checkDigit;
    }

    private static String massageDecodedData(String data, Map< String, Object > okapiConfig) {
        var clazz = okapiConfig.get("okapi_class");
        if (DataBar14.class.getName().equals(clazz)) {
            // remove the checksum from the barcode content
            return data.substring(0, data.length() - 1);
        } else if (DataBarExpanded.class.getName().equals(clazz)) {
            // replace the parenthesis around the GS1 AIs with brackets
            return data.replaceAll("\\((\\d{2,4})\\)", "[$1]");
        }
        // no massaging required
        return data;
    }

    public static String version() {
        try {
            var stream = Reader.class.getResourceAsStream("/META-INF/maven/com.google.zxing/core/pom.properties");
            var props = new Properties();
            props.load(stream);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
