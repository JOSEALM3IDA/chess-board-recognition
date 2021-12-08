package pt.josealm3ida.android.chessboardrecognition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.josealm3ida.android.chessboardrecognition.board.RecyclerViewAdapter

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView : RecyclerView = findViewById(R.id.board_recyclerview)
        val recyclerViewAdapter = RecyclerViewAdapter(this)

        recyclerView.layoutManager = GridLayoutManager(this, Constants.NUM_COLS)
        recyclerView.adapter = recyclerViewAdapter
    }
}