import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Deep Learning Decaptcher.
 * 
 * Uses a CNN Classifier trained in Python Keras with Tensorflow backend.
 * The image is processed using OpenCV to break the captcha into simple characters.
 * Each char is inputed on the classifier, which assigns a probability according the trained model. 
 * 
 * @author gmarinelli
 *
 */
public class DLDecaptcher implements Decaptcher {
	
	public static void main(String[] args) throws IOException {
		DLDecaptcher decaptcher = new DLDecaptcher();

		File imageFile = new File("U:\\CaptchasCVM\\img286.jpg");
		byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
		String captcha = decaptcher.decapcha(imageBytes);
		System.out.println(captcha);
	}

	@Override
	public String decapcha(byte[] captchaImage) {

		CaptchaImageProcessor processor = new CaptchaImageProcessor();
		CaptchaClassifier classifier = new CaptchaClassifier(0.6f);
		String captcha = "";

		try {
			ArrayList<byte[]> captchaChars = processor.process(captchaImage);

			for (byte[] captchaChar : captchaChars) {
				String classifiedChar = classifier.classify(captchaChar);

				if (classifiedChar != null) {
					captcha += classifiedChar;
				}
			}

		} catch (IOException e) {

			e.printStackTrace();
		}

		return captcha;
	}

}
