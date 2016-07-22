package fergaral.datetophoto.utils;

/**
 * Created by fer on 8/07/16.
 */
public class Point {
    private int x, y;

    private enum Corners {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, UNDEFINED;
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private Corners getNearestCorner(int imageWidth, int imageHeight) {
        double marginWidth = (imageWidth * 0.2) / 10;
        double marginHeight = (imageHeight * 0.2) / 10;

        if(x >= (imageWidth - marginWidth)) {
            if(y >= (imageHeight - marginHeight))
                return Corners.BOTTOM_RIGHT;

            if(y >= marginHeight)
                return Corners.TOP_RIGHT;
        }

        if(x >= marginWidth) {
            if(y >= marginHeight)
                return Corners.TOP_LEFT;
            if(y >= (imageHeight - marginHeight))
                return Corners.BOTTOM_LEFT;
        }

        return Corners.UNDEFINED;
    }

    public boolean isInBottomRightCorner(int imageWidth, int imageHeight) {
        return getNearestCorner(imageWidth, imageHeight) == Corners.BOTTOM_RIGHT;
    }
}
