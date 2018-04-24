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
