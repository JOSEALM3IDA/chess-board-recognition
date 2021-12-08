package pt.josealm3ida.android.chessboardrecognition.pieces

class EmptyPiece : Piece {
    companion object {
        const val FIGURINE = ' ';
        const val DRAWABLE = android.R.color.transparent
    }

    override var color = Color.NONE
    override var figurine = FIGURINE
    override var drawable = DRAWABLE
}