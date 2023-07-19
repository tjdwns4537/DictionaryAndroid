package dictionary.dictionaryandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton

class MyListActivity : AppCompatActivity() {

    private lateinit var myListImageBtn: ImageButton
    private lateinit var homeBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_list)
        init()
    }

    private fun init(){
        myListImageBtn = findViewById(R.id.my_list_button)
        homeBtn = findViewById(R.id.home_btn)

        homeBtn.setOnClickListener(View.OnClickListener { View ->
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        })

        myListImageBtn.setOnClickListener(View.OnClickListener { View ->
            val intent = Intent(this, MyListActivity::class.java)
            startActivity(intent)
        })
    }
}