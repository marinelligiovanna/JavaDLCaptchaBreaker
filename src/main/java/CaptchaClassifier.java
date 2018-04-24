

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.tensorflow.Graph;
import org.tensorflow.Output;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

/**
 * Uses a pre-trained model to break a simple numeric Captcha. 
 * This class uses the model trained in Python Keras as defined in my github page: https://github.com/marinelligiovanna/DLCaptchaBreaker
 * Feel free to adapt it for your own models by only changing the name of your pb file, the input node and output node.
 *  
 * @author gmarinelli
 *
 */
public class CaptchaClassifier implements Classifier<String> {

	private static  byte[] modelBytes;
	private static  String[] labels;
	private float threshold;
	
	/**
	 * 
	 * @param threshold Minimum probability to accept a character as answer of classification.
	 */
	public CaptchaClassifier(float threshold) {
		super();
		this.threshold = threshold;
		
	}
	
	static {
		ClassLoader classLoader = CaptchaClassifier.class.getClassLoader();
		
		// Read DL Model
		try {
			File file = new File(classLoader.getResource("CNN_Model/output_graph.pb").getFile());
			modelBytes = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Read labels
		File file = new File(classLoader.getResource("CNN_Model/labels.txt").getFile());
		try (Stream<String> lines = Files.lines(file.toPath())) {
			
			labels = lines.toArray(size -> new String[size]);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Build graph to normalize image to fit the model trained in Python.
	 * 
	 * The model was trained with images scaled to img_shape x img_shape pixels.
	 * The colors, represented as R, G, B in 1-byte each were converted to float using 1/Scale.
	 * 
	 * @param imageBytes
	 * @param height 
	 * @param widht
	 * @param scale
	 * @return
	 */
	private Tensor<Float> normalizedImage(byte[] imageBytes, int height, int widht, float scale) {
		try (Graph g = new Graph()) {
			GraphBuilder b = new GraphBuilder(g);

			final Output<String> input = b.constant("input", imageBytes);
			final Output<Float> output = b.div(b.resizeBilinear(
					b.expandDims(b.cast(b.decodeJpeg(input, 3), Float.class), b.constant("make_batch", 0)),
					b.constant("size", new int[] { height, widht })), b.constant("scale", scale));

			try (Session s = new Session(g)) {
				return s.runner().fetch(output.op().name()).run().get(0).expect(Float.class);
			}
		}
	}

	@Override
	public String classify(byte[] imageBytes) {
		int height = 20;
		int widht = 20;
		float scale = 255f;
		String input_layer = "FirstConv2DLayer_input";
		String output_layer = "conversor_output_0";

		// Create graph based on Protobuffer model generated in Python
		try (Graph graph = new Graph(); Session session = new Session(graph)) {

			graph.importGraphDef(modelBytes);

			// Input normalized image in CNN and gets a vector of probabilities as output
			try (Tensor<Float> image = normalizedImage(imageBytes, height, widht, scale)) {

				try (Session s = new Session(graph);
						Tensor<?> result = s.runner().feed(input_layer, image).fetch(output_layer).run().get(0)) {

					final long[] rshape = result.shape();
					float[] probabilities = (float[]) result.copyTo(new float[(int) rshape[0]]);

					// Find the index of max probability and max value
					int maxAt = 0;
					float maxProb = 0;

					for (int i = 0; i < probabilities.length; i++) {
						maxProb = probabilities[i] > maxProb
								? probabilities[i]
								: maxProb;
						maxAt = probabilities[i] > probabilities[maxAt]
								? i
								: maxAt;
					}

					if (maxProb > threshold) {
						return labels[maxAt];
					}
				}
			}
		}
		return null;
	}

}
