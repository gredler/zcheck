package net.gredler.zcheck;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * @see <a href="http://zint.org.uk/Manual.aspx?type=p&page=4">Zint Manual: Using the Command Line</a>
 */
class Zint {

    public static EncodeResult encode(Map< String, Object > config, String data) {
        try {

            var escapedData = new StringBuilder();
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                if (c <= '\'') {
                    // escape anything that might interfere with the command line:
                    // unprintable characters, single quotes, double quotes, etc
                    escapedData.append("\\x").append(Integer.toHexString(c));
                } else if (c == '\\') {
                    escapedData.append("\\\\");
                } else {
                    escapedData.append(c);
                }
            }

            var command = new ArrayList< String >();
            command.add("lib/zint.exe");
            command.add("--direct");
            for (var entry : config.entrySet()) {
                var name = entry.getKey();
                if (name.startsWith("zint_")) {
                    name = name.substring(5);
                    command.add("--" + name + "=" + entry.getValue());
                }
            }
            command.add("--esc");
            command.add("--data=" + escapedData);
            // TODO: incorporate full Zint command into HTML report somehow?

            var process = new ProcessBuilder(command).start();

            var stderr = process.getErrorStream().readAllBytes();
            if (stderr.length > 0) {
                throw new IOException(new String(stderr, UTF_8));
            }

            var stdout = process.getInputStream();
            var image = ImageIO.read(stdout);
            return new EncodeResult(image, null);

        } catch (IOException e) {
            return new EncodeResult(null, e);
        }
    }

    public static String version() {
        try {
            var process = new ProcessBuilder("lib/zint.exe", "--help").start();
            var stdout = process.getInputStream();
            var output = new String(stdout.readAllBytes(), UTF_8);
            var pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
            var matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group();
            } else {
                throw new IOException("Unable to determine Zint version");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
