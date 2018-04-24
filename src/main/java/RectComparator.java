
import java.util.Comparator;

import org.opencv.core.Rect;

/**
 * Comparator for Rect object of OpenCV
 * 
 * @author gmarinelli
 *
 */
public class RectComparator implements Comparator<Rect> {

	@Override
	public int compare(Rect rect1, Rect rect2) {
		if (rect1.x > rect2.x) {
			return 1;
		} else if (rect1.x < rect2.x) {
			return -1;
		} else {
			return 0;
		}
	}

}
