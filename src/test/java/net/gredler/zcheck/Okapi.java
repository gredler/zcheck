package net.gredler.zcheck;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Map;

import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.output.Java2DRenderer;

class Okapi {

    public static EncodeResult encode(Map< String, Object > config, String data) {
        try {

            var symbol = createSymbol(config);
            symbol.setContent(data);

            int magnification = 2;
            int width = symbol.getWidth() * magnification;
            int height = symbol.getHeight() * magnification;

            var image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            var g2d = image.createGraphics();
            g2d.setPaint(Color.WHITE);
            g2d.fillRect(0, 0, width, height);

            var renderer = new Java2DRenderer(g2d, magnification, Color.WHITE, Color.BLACK);
            renderer.render(symbol);

            g2d.dispose();

            return new EncodeResult(image, null);

        } catch (Exception e) {
            return new EncodeResult(null, e);
        }
    }

    private static Symbol createSymbol(Map< String, Object > config) {
        try {
            var type = (String) config.get("okapi_class");
            var clazz = Class.forName(type);
            var symbol = (Symbol) clazz.getDeclaredConstructor().newInstance();
            for (var entry : config.entrySet()) {
                var name = entry.getKey();
                if (name.startsWith("okapi_") && !"okapi_class".equals(name)) {
                    name = name.substring(6);
                    var value = entry.getValue();
                    var setterName = "set" + snakeCaseToPascalCase(name);
                    var setter = getMethod(clazz, setterName);
                    invoke(symbol, setter, value);
                }
            }
            return symbol;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Unable to create symbol", e);
        }
    }

    private static String snakeCaseToPascalCase(String s) {
        var sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        for (int index = sb.indexOf("_"); index != -1; index = sb.indexOf("_")) {
            sb.deleteCharAt(index);
            sb.setCharAt(index, Character.toUpperCase(sb.charAt(index)));
        }
        return sb.toString();
    }

    private static Method getMethod(Class< ? > clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method named " + name);
    }

    @SuppressWarnings("unchecked")
    private static < E extends Enum< E > > void invoke(Object object, Method setter, Object parameter)
                    throws ReflectiveOperationException, IllegalArgumentException {

        Object paramValue;

        if (parameter == null) {
            paramValue = null;
        } else {
            Class< ? > paramType = setter.getParameterTypes()[0];
            if (String.class.equals(paramType)) {
                paramValue = parameter.toString();
            } else if (boolean.class.equals(paramType)) {
                paramValue = Boolean.valueOf(parameter.toString());
            } else if (int.class.equals(paramType)) {
                paramValue = Integer.parseInt(parameter.toString());
            } else if (double.class.equals(paramType)) {
                paramValue = Double.parseDouble(parameter.toString());
            } else if (Character.class.equals(paramType)) {
                paramValue = parameter.toString().charAt(0);
            } else if (paramType.isEnum()) {
                Class< E > e = (Class< E >) paramType;
                paramValue = Enum.valueOf(e, parameter.toString());
            } else {
                throw new RuntimeException("Unknown setter type: " + paramType);
            }
        }

        setter.invoke(object, paramValue);
    }

    public static String version() {
        return Symbol.class.getPackage().getImplementationVersion();
    }
}
