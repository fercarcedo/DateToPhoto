package fergaral.datetophoto.utils

/**
 * Created by fer on 8/07/16.
 */
class Point(val x: Int, val y: Int) {

    private enum class Corners {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, UNDEFINED
    }

    private fun getNearestCorner(imageWidth: Int, imageHeight: Int): Corners {
        val marginWidth = imageWidth * 0.2 / 10
        val marginHeight = imageHeight * 0.2 / 10

        if (x >= imageWidth - marginWidth) {
            if (y >= imageHeight - marginHeight)
                return Corners.BOTTOM_RIGHT

            if (y >= marginHeight)
                return Corners.TOP_RIGHT
        }

        if (x >= marginWidth) {
            if (y >= marginHeight)
                return Corners.TOP_LEFT
            if (y >= imageHeight - marginHeight)
                return Corners.BOTTOM_LEFT
        }

        return Corners.UNDEFINED
    }

    fun isInBottomRightCorner(imageWidth: Int, imageHeight: Int): Boolean {
        return getNearestCorner(imageWidth, imageHeight) == Corners.BOTTOM_RIGHT
    }
}
