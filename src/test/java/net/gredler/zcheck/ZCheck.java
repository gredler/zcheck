package net.gredler.zcheck;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.yaml.snakeyaml.Yaml;

import com.mifmif.common.regex.Generex;

// TODO: possibly parallelize using ForkJoinPool or Executors.newWorkStealingPool()

public class ZCheck {

    public static void main(String[] args) throws Exception {

        // it is possible to re-run previous tests by using the random seed that was previously used
        var seed = (args.length > 0 ? Long.parseLong(args[0]) : System.nanoTime());
        var random = new Random(seed);
        var config = readTestConfig();
        var tests = new ArrayList< Test >();
        var id = 1;

        for (var barcode : config.barcodes()) {
            for (var pattern : barcode.patterns()) {
                System.out.println("Testing " + barcode.name() + " using pattern '" + pattern + "'...");
                var generator = new Generex(pattern, random);
                for (int i = 0; i < config.testsPerPattern(); i++) {
                    var data = generator.random();
                    var okapiEncodeResult = Okapi.encode(barcode.okapiConfig(), data);
                    var okapiDecodeResult = ZXing.decode(okapiEncodeResult.image(), data, barcode.okapiConfig());
                    var zintEncodeResult = Zint.encode(barcode.zintConfig(), data);
                    var zintDecodeResult = ZXing.decode(zintEncodeResult.image(), data, barcode.okapiConfig());
                    var test = new Test(id++, barcode, data, okapiEncodeResult.withoutImage(), okapiDecodeResult, zintEncodeResult.withoutImage(), zintDecodeResult);
                    tests.add(test);
                }
            }
        }

        var testSuite = new TestSuite(seed, tests);
        Report.generate(testSuite);
    }

    @SuppressWarnings("unchecked")
    private static TestConfig readTestConfig() {
        System.out.println("Loading test configuration...");
        var stream = ZCheck.class.getResourceAsStream("tests.yaml");
        var yaml = (Map< String, Object >) new Yaml().load(stream);
        var tests = (Integer) yaml.remove("tests_per_pattern");
        var configs = (List< Map< String, Object > >) yaml.remove("barcodes");
        var barcodes = configs.stream().map(ZCheck::toBarcode).filter(Objects::nonNull).collect(toList());
        if (!yaml.isEmpty()) {
            throw new IllegalArgumentException("Unknown configuration keys: " + yaml.keySet());
        }
        return new TestConfig(barcodes, tests);
    }

    @SuppressWarnings("unchecked")
    private static Barcode toBarcode(Map< String, Object > config) {
        var name = (String) config.remove("name");
        var disabled = config.remove("disabled_because");
        if (disabled != null) {
            System.out.println("Skipping " + name + " (disabled because " + disabled + ")...");
            return null;
        }
        var patterns = (List< String >) config.remove("patterns");
        var okapiConfig = removeAll(config, "okapi_");
        var zintConfig = removeAll(config, "zint_");
        if (!config.isEmpty()) {
            throw new IllegalArgumentException("Unknown configuration keys: " + config.keySet());
        }
        return new Barcode(name, okapiConfig, zintConfig, patterns);
    }

    private static Map< String, Object > removeAll(Map< String, Object > map, String prefix) {
        var ret = map.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.keySet().removeAll(ret.keySet());
        return Collections.unmodifiableMap(ret);
    }
}
