package pt.josealm3ida.android.chesser

import org.apache.commons.math3.ml.clustering.Clusterable
import org.opencv.core.Point

class ClusterablePoint : Point(), Clusterable {
    override fun getPoint(): DoubleArray {
        return doubleArrayOf(x, y)
    }
}