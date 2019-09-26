package com.web.common;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * <p>boofcv解析二维码 效果比zxing好</p>
 *
 * @author JingHe
 * @version 1.0
 * @since 2019/9/26
 */

public class QrCodeBfUtil {

	/**
	 * boofcv解析二维码
	 * @param filePath 文件路径
	 * @return
	 */
	public static List<QrCode> decode(String filePath) {
		BufferedImage input = UtilImageIO.loadImage(UtilIO.pathExample(filePath));
		GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);
		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null,GrayU8.class);
		detector.process(gray);
		return detector.getDetections();
	}
}
