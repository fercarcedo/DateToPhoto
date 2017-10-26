package fergaral.datetophoto.algorithms;

import java.io.Serializable;

/**
 * Created by Fer on 18/10/2017.
 */

public interface PhotoProcessedAlgorithm extends Serializable {
    enum Result { PROCESSED, NOT_PROCESSED, ERROR };
    Result isProcessed(String photoPath);
}
