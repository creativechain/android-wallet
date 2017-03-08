package crea.wallet.lite.widget.qrscanner;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

import me.dm7.barcodescanner.core.DisplayUtils;

/**
 * Created by ander on 31/08/15.
 */
public class ScannerView extends AbstractScannerView {

    private MultiFormatReader mMultiFormatReader;
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
    private List<BarcodeFormat> mFormats;
    private ResultHandler mResultHandler;

    public ScannerView(Context context) {
        super(context);
        this.initMultiFormatReader();
    }

    public ScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.initMultiFormatReader();
    }

    public void setFormats(List<BarcodeFormat> formats) {
        this.mFormats = formats;
        this.initMultiFormatReader();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        return this.mFormats == null?ALL_FORMATS:this.mFormats;
    }

    private void initMultiFormatReader() {
        EnumMap hints = new EnumMap(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, this.getFormats());
        this.mMultiFormatReader = new MultiFormatReader();
        this.mMultiFormatReader.setHints(hints);
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;
        if(DisplayUtils.getScreenOrientation(this.getContext()) == 1) {
            byte[] rawResult = new byte[data.length];

            int source;
            for(source = 0; source < height; ++source) {
                for(int bitmap = 0; bitmap < width; ++bitmap) {
                    rawResult[bitmap * height + height - source - 1] = data[bitmap + source * width];
                }
            }

            source = width;
            width = height;
            height = source;
            data = rawResult;
        }

        Result var20 = null;
        PlanarYUVLuminanceSource var21 = this.buildLuminanceSource(data, width, height);
        if(var21 != null) {
            BinaryBitmap var22 = new BinaryBitmap(new HybridBinarizer(var21));

            try {
                var20 = this.mMultiFormatReader.decodeWithState(var22);
            } catch (ReaderException var16) {
            } catch (NullPointerException var17) {
            } catch (ArrayIndexOutOfBoundsException var18) {
            } finally {
                this.mMultiFormatReader.reset();
            }
        }

        if(var20 != null) {
            this.stopCamera();
            if(this.mResultHandler != null) {
                this.mResultHandler.handleResult(var20);
            }
        } else {
            camera.setOneShotPreviewCallback(this);
        }

    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = this.getFramingRectInPreview(width, height);
        if(rect == null) {
            return null;
        } else {
            PlanarYUVLuminanceSource source = null;

            try {
                source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
            } catch (Exception var7) {
            }

            return source;
        }
    }

    static {
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
    }

    public interface ResultHandler {
        void handleResult(Result var1);
    }
}
