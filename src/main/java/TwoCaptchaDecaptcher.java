
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Scanner;

/**
 * 2Captcha is a human-powered image and CAPTCHA recognition service. 2Captcha's
 * main purpose is solving your CAPTCHAs in a quick and accurate way by human
 * employees, but the service is not limited only to CAPTCHA solving. You can
 * convert to text any image that a human can recognize.
 * 
 * @author gmarinelli
 *
 */
public class TwoCaptchaDecaptcher implements Decaptcher {

	private final String api_key;

	/**
	 * Creates a new 2captcha decaptcher with your API key; Get your API key from
	 * your account settings page. Each user is given a unique authentication token,
	 * we call it API key. It's a 32-characters string that looks like:
	 * 1abc234de56fab7c89012d34e56fa7b8
	 * 
	 * @param api_key
	 */
	public TwoCaptchaDecaptcher(String api_key) {
		this.api_key = api_key;
	}

	/**
	 * We provide an API that allows you to automate the process and integrate your
	 * software with our service. Identifies the text in the captcha image;
	 */
	public String decapcha(byte[] captchaImage) {
		try {
			String id = sendImage(captchaImage);
			System.out.println("Got id from 2captcha: " + id);

			String text = "CAPCHA_NOT_READY";
			while (text.equalsIgnoreCase("CAPCHA_NOT_READY")) {
				// Make a timeout: 20 seconds for ReCaptcha, 5 seconds for other types of
				// captchas.
				Thread.sleep(1000);
				text = getText(id);
				System.out.println("Got text from 2captcha: " + text);
			}

			return text;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Submit a HTTP POST request to our API URL: http://2captcha.com/in.php with
	 * parameters corresponding to the type of your captcha. Server will return
	 * captcha ID or an error code if something went wrong. If everything is fine
	 * server will return the ID of your captcha as plain text, like: OK|2122988149
	 * or as JSON {"status":1,"request":"2122988149"} if json parameter was used.
	 * 
	 * @param captchaImage
	 * @return
	 * @throws IOException
	 */
	private String sendImage(byte[] captchaImage) throws IOException {

		String captchaBase64 = Base64.getEncoder().encodeToString(captchaImage);
		StringBuilder postData = new StringBuilder(2048);
		postData.append("method=base64");
		postData.append("&key=");
		postData.append(URLEncoder.encode(this.api_key, "UTF-8"));
		postData.append("&body=");
		postData.append(URLEncoder.encode(captchaBase64, "UTF-8"));
		byte[] postDataBytes = postData.toString().getBytes("UTF-8");

		URL target = new URL("http://2captcha.com/in.php");
		HttpURLConnection connection = (HttpURLConnection) target.openConnection();

		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		OutputStream os = connection.getOutputStream();
		try {
			os.write(postDataBytes);
		} finally {
			os.close();
		}
		InputStream is = connection.getInputStream();
		try {
			Scanner s = new Scanner(is);
			s.useDelimiter("\\A");
			try {
				String result = s.hasNext() ? s.next() : "";
				System.out.println(result);
				if (result.startsWith("OK|"))
					return result.substring(3);
				else
					throw new RuntimeException(result);
			} finally {
				s.close();
			}
		} finally {
			is.close();
		}
	}

	/**
	 * Submit a HTTP GET request to our API URL: http://2captcha.com/res.php to get
	 * the result. If captha is already solved server will return the answer in
	 * format corresponding to the type of your captcha. By default answers are
	 * returned as plain text like: OK|Your answer. But answer can also be returned
	 * as JSON {"status":1,"request":"TEXT"} if json parameter is used. If captcha
	 * is not solved yet server will return CAPCHA_NOT_READY result. Repeat your
	 * request in 5 seconds. If something went wrong server will return an error
	 * code.
	 * 
	 * @param reqId
	 * @return
	 * @throws IOException
	 */
	private String getText(String reqId) throws IOException {

		String endpoint = "http://2captcha.com/res.php?key=";
		endpoint += URLEncoder.encode(this.api_key, "UTF-8");
		endpoint += "&action=get";
		endpoint += "&id=";
		endpoint += URLEncoder.encode(reqId, "UTF-8");

		URL url = new URL(endpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		InputStream is = connection.getInputStream();
		try {
			Scanner s = new Scanner(is);
			s.useDelimiter("\\A");
			try {
				String result = s.hasNext() ? s.next() : "";
				System.out.println(result);
				if (result.startsWith("OK|"))
					return result.substring(3);
				else if (result.equalsIgnoreCase("CAPCHA_NOT_READY"))
					return result;
				else
					throw new RuntimeException(result);
			} finally {
				s.close();
			}
		} finally {
			is.close();
		}

	}
}
