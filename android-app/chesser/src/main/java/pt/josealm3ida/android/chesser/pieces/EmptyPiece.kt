package pt.josealm3ida.android.chesser.pieces

class EmptyPiece : Piece {
    companion object {
        const val FIGURINE = ' ';
        const val DRAWABLE = android.R.drawable.ic_delete
    }

    override val pieceColor = PieceColor.NONE
    override val figurine = FIGURINE
    override val drawable = DRAWABLE
}