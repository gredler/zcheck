package net.gredler.zcheck;

import java.util.List;
import java.util.Map;

record Barcode(String name, Map< String, Object > okapiConfig, Map< String, Object > zintConfig, List< String > patterns) {

}
