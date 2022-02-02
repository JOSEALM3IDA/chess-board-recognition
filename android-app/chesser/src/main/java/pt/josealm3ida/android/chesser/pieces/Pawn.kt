package pt.josealm3ida.android.chesser.pieces

import pt.josealm3ida.android.chesser.R

class Pawn(override val pieceColor: PieceColor) : Piece {
    companion object {
        const val FIGURINE_WHITE = '♙';
        const val FIGURINE_BLACK = '♟';

        const val DRAWABLE_WHITE = R.drawable.ic_pawn_white
        const val DRAWABLE_BLACK = R.drawable.ic_pawn_black
    }

    override val figurine: Char = if (pieceColor == PieceColor.WHITE) FIGURINE_WHITE else FIGURINE_BLACK
    override val drawable : Int = if (pieceColor == PieceColor.WHITE) DRAWABLE_WHITE else DRAWABLE_BLACK

}