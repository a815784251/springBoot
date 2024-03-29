package com.web.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>二维码工具类</p>
 *
 * @author JingHe
 * @version 1.0
 * @since 2019/8/26
 */
public class QRCodeUtil {

    private static final int DEFAULT_WIDTH = 200;

    private static final int DEFAULT_HEIGHT = 200;

    private static final String CHARACTER_SET = "UTF-8";

    private static final String DEFAULT_FILE_SUFFIX = "png";

    /**
     * 解析二维码
     * @param filepath path
     * @return
     * @throws IOException
     * @throws NotFoundException
     */
    public static String decode(String filepath) throws IOException, NotFoundException {
        InputStream inputStream = new FileInputStream(filepath);
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        Binarizer binarizer = new HybridBinarizer(source);
        BinaryBitmap bitmap = new BinaryBitmap(binarizer);
        MultiFormatReader decodeReader = new MultiFormatReader();
        Result result = null;
        try {
            result = decodeReader.decodeWithState(bitmap);
        } catch (Exception e) {
            HashMap<DecodeHintType, Object> decodeHints = new HashMap<>();
            //编码设置
            decodeHints.put(DecodeHintType.CHARACTER_SET, CHARACTER_SET);
            //优化精度
            decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            //复杂模式，开启PURE_BARCODE模式
            decodeHints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
            result = decodeReader.decode(bitmap, decodeHints);
        }
        inputStream.close();
        return result == null ? "" : result.getText();
    }

    /**
     * 生成二维码
     * @param content 内容
     * @param filepath 路径
     * @throws WriterException
     * @throws IOException
     */
    public static void encode(String content, String filepath) throws WriterException, IOException {
        Map<EncodeHintType, Object> encodeHints = new HashMap<>();
        encodeHints.put(EncodeHintType.CHARACTER_SET, CHARACTER_SET);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, DEFAULT_WIDTH, DEFAULT_HEIGHT, encodeHints);
        Path path = FileSystems.getDefault().getPath(filepath);
        MatrixToImageWriter.writeToPath(bitMatrix, DEFAULT_FILE_SUFFIX, path);
    }

    @Test
    public void test() throws Exception {
        long time = System.currentTimeMillis();
        String path = "E:\\picture\\s2.png";
        String secretStr = decode(path);
        System.out.println(secretStr);
        System.out.println(System.currentTimeMillis() - time);
        encode(secretStr, "E:\\picture\\s5.png");
    }

}
