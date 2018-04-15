package xyz.kurozero.colors

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Random
import android.graphics.PorterDuff
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import android.support.v4.graphics.ColorUtils

class MainActivity : AppCompatActivity() {

    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                imageView.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                val hexColor = String.format("#%06X", 0xFFFFFF and color)
                colorTextView.text = hexColor
            }
        })

        randomColorButton.setOnClickListener({
            val color = ColorUtils.HSLToColor(floatArrayOf(random.nextFloat(), random.nextFloat(), random.nextFloat()))
            val hexColor = String.format("#%06X", 0xFFFFFF and color)
            randomColorButton.setBackgroundColor(color)
            imageView.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
            colorPicker.setColor(color)
            colorTextView.text = hexColor
        })
    }
}
