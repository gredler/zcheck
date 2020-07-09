package net.gredler.zcheck;

import java.util.List;

record TestConfig(List< Barcode > barcodes, int testsPerPattern) {

}
