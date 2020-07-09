package net.gredler.zcheck;

record DecodeResult(String encodedData, String decodedData, Exception error) {

    public boolean successful() {
        return error == null && decodedData != null && decodedData.equals(encodedData);
    }

    @Override
    public String toString() {
        if (error != null) {
            return error.getMessage() != null ? error.getMessage() : error.getClass().getCanonicalName();
        } else if (decodedData == null) {
            return "No decoded data";
        } else if (!decodedData.equals(encodedData)) {
            return encodedData + " != " + decodedData;
        } else {
            return "OK";
        }
    }
}
