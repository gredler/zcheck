package net.gredler.zcheck;

record Test(int id, Barcode barcode, String data,
            EncodeResult okapiEncodeResult, DecodeResult okapiDecodeResult,
            EncodeResult zintEncodeResult, DecodeResult zintDecodeResult) {

    public boolean successful() {
        return okapiEncodeResult.successful() &&
               okapiDecodeResult.successful() &&
               zintEncodeResult.successful() &&
               zintDecodeResult.successful();
    }

}
