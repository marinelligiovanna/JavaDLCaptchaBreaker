
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * Process captcha image to fit the CNN Model trained in Python using OpenCV.
 * 
 * @author gmarinelli
 *
 */
public class CaptchaImageProcessor {

	public CaptchaImageProcessor() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	/**
	 * Save an image from byte[]
	 * 
	 * @param imageBytes
	 *            byte[] containing the image to be saved
	 * @param pathToSave
	 *            path to save the image, including the image name and format
	 * @throws IOException
	 */
	private static void saveImageFromByteArray(byte[] imageBytes, String pathToSave) throws IOException {

		FileOutputStream fos = new FileOutputStream(pathToSave);
		try {
			fos.write(imageBytes);
		} finally {
			fos.close();
		}
	}

	/**
	 * Convert Mat to byte[].
	 * 
	 * @param matrix
	 * @return Mat converted to byte[]
	 * @throws IOException
	 */
	public static byte[] mat2byteArray(Mat matrix) throws IOException {

		// http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;

		matrix.get(0, 0, data);

		switch (matrix.channels()) {

		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;

		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;

		default:
			return null;
		}

		BufferedImage image = new BufferedImage(cols, rows, type);
		image.getRaster().setDataElements(0, 0, cols, rows, data);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpg", baos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return baos.toByteArray();

	}

	/**
	 * Convert byte[] to Mat.
	 * 
	 * @param byteArray
	 * @return byte[] converted
	 */
	public static Mat byteArray2Mat(byte[] byteArray) {
		return Highgui.imdecode(new MatOfByte(byteArray), Highgui.CV_LOAD_IMAGE_UNCHANGED);
	}

	/**
	 * Convert Mat to Mat with entries in 32F format. Necessary for K-Means method.
	 * 
	 * @param image
	 * @return Mat in 32F format
	 */
	public static Mat convertMatTo32F(Mat image) {

		// Reshape image to be a list of pixels
		Mat reshaped_image = image.reshape(1, image.cols() * image.rows());

		// Convert list to float 32
		Mat reshaped_image32f = new Mat();
		reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0);

		return reshaped_image32f;
	}

	/**
	 * Use K-means algorithm to verify if a thresholded image has a black or white
	 * background.
	 * 
	 * Reference: http://aishack.in/tutorials/kmeans-clustering-opencv/
	 * 
	 * @param image
	 * @return boolean specifying if background is black (true) or white (false)
	 */
	private boolean hasBlackBackgroud(Mat image) {

		Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2BGR);
		Mat image32F = convertMatTo32F(image);

		Mat labels = new Mat();
		TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 50, 0.1);
		Mat centers = new Mat();
		int clusterCount = 1;
		int attempts = 1;

		// Cluster the pixel intensities
		Core.kmeans(image32F, clusterCount, labels, criteria, attempts, Core.KMEANS_PP_CENTERS, centers);

		// Get center color. We only need to look for 1-d because our image is black and
		// white
		double centerColor = centers.get(0, 0)[0];

		if (centerColor < 100) {
			return true;
		}

		return false;
	}

	/**
	 * Invert colors of Mat
	 * 
	 * @param matrix
	 * @return
	 */
	private Mat invertColors(Mat matrix) {

		// Copy mat before invert colors
		Mat copyMat = new Mat();
		matrix.copyTo(copyMat);

		// Invert
		Mat invertcolormatrix = new Mat(copyMat.rows(), copyMat.cols(), copyMat.type(), new Scalar(255, 255, 255));
		Core.subtract(invertcolormatrix, copyMat, copyMat);

		return copyMat;
	}

	/**
	 * Apply Otsu's thresholding in captcha image for better character
	 * identification. Also applies other transformation to improve the thresholding
	 * result. Based on Python implementation.
	 * 
	 * @param imageMat
	 *            captcha image in Mat format
	 * @return Thresholded image
	 * @throws IOException
	 */
	private Mat thresholdCaptchaImage(Mat imageMat) throws IOException {

		// Mat imageMat = byteArray2Mat(imageBytes);

		// Convert image to grayscale and resize for a better result
		Size originalImageSize = imageMat.size();
		Imgproc.resize(imageMat, imageMat, new Size(0, 0), 5.0, 5.0, Imgproc.INTER_CUBIC);
		Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
		Imgproc.copyMakeBorder(imageMat, imageMat, 8, 8, 8, 8, Imgproc.BORDER_REPLICATE);

		// Gaussian filter and Otsu's thresholding
		Mat imageBlur = new Mat();
		Mat imageThresh = new Mat();
		Imgproc.GaussianBlur(imageMat, imageBlur, new Size(7, 7), 0);
		Imgproc.threshold(imageBlur, imageThresh, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

		// Erode, dilate and closing to remove noise
		Mat kernel = Mat.eye(new Size(5, 5), CvType.CV_8U);
		Imgproc.erode(imageThresh, imageThresh, kernel);
		Imgproc.dilate(imageThresh, imageThresh, kernel);
		Imgproc.morphologyEx(imageThresh, imageThresh, Imgproc.MORPH_CLOSE, kernel);

		// Resize image back to its original size
		Imgproc.resize(imageThresh, imageThresh, originalImageSize);

		// Invert color if image has black backgroud (more suitable to ML algorithm)
		if (hasBlackBackgroud(imageThresh)) {
			imageThresh = invertColors(imageThresh);
			Core.bitwise_not(imageThresh, imageThresh);
		}

		return imageThresh;
	}

	/**
	 * Segment a thresholded captcha image using contours method. Find the bounding
	 * rectangle of each contour and crop the image according.
	 * 
	 * @param imageMat
	 *            thresholded image
	 * @param minHeight
	 *            minimum height of bouding rectangle
	 * @param minWidth
	 *            minimum widht of bouding rectangle
	 * @return Arraylist containing the segmented characters.
	 */
	private ArrayList<byte[]> segmentCaptchaImage(Mat imageMat, double minHeight, double minWidth) {

		// Find contours in negative image to avoid finding image borders;
		Mat invImageMat = invertColors(imageMat);
		Highgui.imwrite("U:\\norm.jpg", imageMat);
		Highgui.imwrite("U:\\inv.jpg", invImageMat);
		Imgproc.cvtColor(invImageMat, invImageMat, Imgproc.COLOR_RGB2GRAY);
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(invImageMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

		// Find bounding rectangles based on contours
		List<Rect> boundingRectangles = new ArrayList<>();

		for (int i = 0; i < contours.size(); i++) {
			Rect rect = Imgproc.boundingRect(contours.get(i));

			// Save only rectangles in ROI
			if (rect.height > minHeight && rect.width > minWidth) {
				boundingRectangles.add(rect);
			}
		}

		// Sort rectangles by x-axis coordinate and crop Mat based on it
		Collections.sort(boundingRectangles, new RectComparator());

		ArrayList<byte[]> segmentedChars = new ArrayList<>();
		for (int i = 0; i < boundingRectangles.size(); i++) {

			try {
				Mat croppedRectMat = imageMat.submat(boundingRectangles.get(i));
				// System.out.println(imageMat.channels());
				segmentedChars.add(mat2byteArray(croppedRectMat));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return segmentedChars;
	}

	/**
	 * Process captcha image and segment its characters
	 * 
	 * @param captchaImageBytes
	 * @return Arraylist of byte[] containing the segmented characters of captcha.
	 * @throws IOException
	 */
	public ArrayList<byte[]> process(byte[] captchaImageBytes) throws IOException {

		Mat captchaImageMat = byteArray2Mat(captchaImageBytes);
		Mat threshCaptcha = thresholdCaptchaImage(captchaImageMat);

		double minHeight = 5;
		double minWidth = 5;

		ArrayList<byte[]> segmentedChars = segmentCaptchaImage(threshCaptcha, minHeight, minWidth);

		return segmentedChars;
	}

	public static void main(String[] args) throws IOException {

		String dirToSave = "U:\\";
		String captchaPath = "U:\\RandomTxt.jpg";
		File imageFile = new File(captchaPath);
		byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

		CaptchaImageProcessor processor = new CaptchaImageProcessor();
		List<byte[]> segmentedChars = processor.process(imageBytes);

		for (int i = 0; i < segmentedChars.size(); i++) {
			saveImageFromByteArray(segmentedChars.get(i), dirToSave + "Char_" + i + ".jpg");
		}

	}

}
