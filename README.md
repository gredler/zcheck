### ZCheck

A fuzz tester for [Zint](http://zint.org.uk/) and [Okapi](https://github.com/woo-j/OkapiBarcode).

Test cases are defined in [tests.yaml](src/test/resources/net/gredler/zcheck/tests.yaml).

Test data is generated randomly using [Generex](https://github.com/mifmif/generex), based on regular expressions which define data format constraints.

Results are checked against [ZXing](https://github.com/zxing/zxing) for correctness.

Test results are summarized in an HTML report. Past test results are available in the [reports](reports) directory.

```
                                             +---------+        +---------+
                          +---------+        |  Okapi  |        |  ZXing  |
    +------------+        |         | -----> |  encode | -----> |  decode |        +-------------+
    |            |        |         |        +---------+        +---------+        |             |
    | tests.yaml | -----> | Generex |                                       -----> | HTML report |
    |            |        |         |        +---------+        +---------+        |             |
    +------------+        |         | -----> |  Zint   | -----> |  ZXing  |        +-------------+
                          +---------+        |  encode |        |  decode |
                                             +---------+        +---------+
```

### Running

`gradlew zcheck`: Runs the tests and generates a report in `reports/yyyy-mm-dd-seed.html`

`gradlew zcheck --args=<seed>`: Runs the tests using the specified RNG seed (allows re-running a previous test set)

### Limitations

Quite a few barcode types are not being tested, either because they haven't been configured yet or because ZXing does not decode the relevant barcode type.

We are not currently cross-checking the Zint and Okapi encoding results, only checking that they decode to the same data which was originally encoded.

Given a character length constraint like `[A-Z]{1,50}` Generex [will usually](https://github.com/mifmif/Generex/issues/51) generate a very short string. Longer strings are unfortunately rare.

Although many of the configured data patterns allow control characters, Generex does not seem to generate such characters.

ZXing is by no means bug-free. Test failures may sometimes be caused by issues in the decoder (ZXing), rather than issues in the encoders (Okapi, Zint).
