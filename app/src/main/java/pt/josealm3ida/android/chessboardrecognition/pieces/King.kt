package pt.josealm3ida.android.chessboardrecognition.pieces

import pt.josealm3ida.android.chessboardrecognition.R

class King(override var color: Color) : Piece {
    companion object {
        const val FIGURINE_WHITE = '♔';
        const val FIGURINE_BLACK = '♚';

        const val DRAWABLE_WHITE = R.drawable.ic_king_white
        const val DRAWABLE_BLACK = R.drawable.ic_king_black
    }

    override var figurine: Char = '?'
    override var drawable : Int = -1

    init {
        figurine = if (color == Color.WHITE) FIGURINE_WHITE else FIGURINE_BLACK
        drawable = if (color == Color.WHITE) DRAWABLE_WHITE else DRAWABLE_BLACK
    }

}