package net.gredler.zcheck;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

class Report {

    private static final String ROWS = "{rows}";
    private static final String DATE = "{date}";
    private static final String SEED = "{seed}";
    private static final String TOTAL = "{total}";
    private static final String PASSED = "{passed}";
    private static final String FAILED = "{failed}";
    private static final String OKAPI_PASSED = "{okapiPassed}";
    private static final String OKAPI_FAILED = "{okapiFailed}";
    private static final String ZINT_PASSED = "{zintPassed}";
    private static final String ZINT_FAILED = "{zintFailed}";
    private static final String PASSED_PERCENT = "{passedPercent}";
    private static final String FAILED_PERCENT = "{failedPercent}";
    private static final String OKAPI_PASSED_PERCENT = "{okapiPassedPercent}";
    private static final String OKAPI_FAILED_PERCENT = "{okapiFailedPercent}";
    private static final String ZINT_PASSED_PERCENT = "{zintPassedPercent}";
    private static final String ZINT_FAILED_PERCENT = "{zintFailedPercent}";
    private static final String OKAPI_VERSION = "{okapiVersion}";
    private static final String ZINT_VERSION = "{zintVersion}";
    private static final String ZXING_VERSION = "{zxingVersion}";

    public static void generate(TestSuite testSuite) {
        try {

            var templateStream = ZCheck.class.getResourceAsStream("template.html");
            var templateBytes = templateStream.readAllBytes();
            var template = new String(templateBytes, UTF_8);

            var rows = new StringBuilder();
            for (var test : testSuite.tests()) {
                rows.append("<tr>");
                rows.append("<td>").append(test.id()).append("</td>");
                rows.append("<td>").append(test.successful() ? "Pass" : "Fail").append("</td>");
                rows.append("<td>").append(test.barcode().name()).append("</td>");
                rows.append("<td>").append(preprocessData(test.data())).append("</td>");
                rows.append("<td>").append(test.okapiEncodeResult()).append("</td>");
                rows.append("<td>").append(test.okapiDecodeResult()).append("</td>");
                rows.append("<td>").append(test.zintEncodeResult()).append("</td>");
                rows.append("<td>").append(test.zintDecodeResult()).append("</td>");
                rows.append("</tr>\n");
            }

            var tests = testSuite.tests();
            var total = tests.size();
            var passed = tests.stream().filter(r -> r.successful()).count();
            var failed = total - passed;
            var okapiPassed = tests.stream().filter(r -> r.okapiEncodeResult().successful() && r.okapiDecodeResult().successful()).count();
            var okapiFailed = total - okapiPassed;
            var zintPassed = tests.stream().filter(r -> r.zintEncodeResult().successful() && r.zintDecodeResult().successful()).count();
            var zintFailed = total - zintPassed;

            var percentFormat = new DecimalFormat("#.#");
            var passedPercent = 100 * passed / total;
            var failedPercent = 100 * failed / total;
            var okapiPassedPercent = 100 * okapiPassed / total;
            var okapiFailedPercent = 100 * okapiFailed / total;
            var zintPassedPercent = 100 * zintPassed / total;
            var zintFailedPercent = 100 * zintFailed / total;
            var date = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());

            var html = template.replace(DATE, date)
                               .replace(SEED, String.valueOf(testSuite.seed()))
                               .replace(TOTAL, String.valueOf(total))
                               .replace(PASSED, String.valueOf(passed))
                               .replace(FAILED, String.valueOf(failed))
                               .replace(OKAPI_PASSED, String.valueOf(okapiPassed))
                               .replace(OKAPI_FAILED, String.valueOf(okapiFailed))
                               .replace(ZINT_PASSED, String.valueOf(zintPassed))
                               .replace(ZINT_FAILED, String.valueOf(zintFailed))
                               .replace(PASSED_PERCENT, percentFormat.format(passedPercent))
                               .replace(FAILED_PERCENT, percentFormat.format(failedPercent))
                               .replace(OKAPI_PASSED_PERCENT, percentFormat.format(okapiPassedPercent))
                               .replace(OKAPI_FAILED_PERCENT, percentFormat.format(okapiFailedPercent))
                               .replace(ZINT_PASSED_PERCENT, percentFormat.format(zintPassedPercent))
                               .replace(ZINT_FAILED_PERCENT, percentFormat.format(zintFailedPercent))
                               .replace(OKAPI_VERSION, Okapi.version())
                               .replace(ZINT_VERSION, Zint.version())
                               .replace(ZXING_VERSION, ZXing.version())
                               .replace(ROWS, rows);

            var dir = Paths.get("reports");
            Files.createDirectories(dir);

            var filename = date + "-" + testSuite.seed() + ".html";
            var path = dir.resolve(filename);
            Files.writeString(path, html, UTF_8);
            System.out.println("Generated report: " + path);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String preprocessData(String data) {
        return "\"" + data.replace(" ", "&nbsp;").replace("<", "&lt;").replace(">", "&gt;") + "\"";
    }
}
