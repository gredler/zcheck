package net.gredler.zcheck;

import java.awt.image.BufferedImage;

record EncodeResult(BufferedImage image, Exception error) {

    public EncodeResult withoutImage() {
        // we can't run millions of tests if we're keeping the image contents in memory for every test longer than we need to
        return new EncodeResult(null, error);
    }

    public boolean successful() {
        return error == null;
    }

    @Override
    public String toString() {
        return successful() ? "OK" : error.getMessage();
    }
}
