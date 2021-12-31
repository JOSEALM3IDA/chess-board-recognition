package pt.josealm3ida.android.chesser.pieces

class EmptyPiece : Piece {
    companion object {
        const val FIGURINE = ' ';
        const val DRAWABLE = android.R.color.transparent
    }

    override val color = Color.NONE
    override val figurine = FIGURINE
    override val drawable = DRAWABLE
}