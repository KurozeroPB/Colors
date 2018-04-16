package xyz.kurozero.colors

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.support.v4.graphics.ColorUtils
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.view.KeyEvent
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.icu.math.BigDecimal

import java.util.Random
import java.lang.Integer.parseInt

import kotlinx.android.synthetic.main.activity_main.*
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import org.jetbrains.anko.*


class MainActivity : AppCompatActivity() {

    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        colorPicker.setColorSelectionListener(object : SimpleColorSelectionListener() {
            @SuppressLint("SetTextI18n")
            override fun onColorSelected(color: Int) {
                imageView.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
                val hexColor = String.format("#%06X", 0xFFFFFF and color)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                textViewRGB.text = "RGB: $r, $g, $b"

                val hsl = FloatArray(3)
                ColorUtils.RGBToHSL(r, g, b, hsl)
                textViewHSL.text = "HSL: ${round(hsl[0].toDouble())}, ${round(hsl[1].toDouble())}, ${round(hsl[2].toDouble())}"

                colorTextInput.setText(hexColor)
                colorTextInput.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                setStatusBarColor(color)
                supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
            }
        })

        randomColorButton.setOnClickListener({
            val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
            val hexColor = String.format("#%06X", 0xFFFFFF and color)
            colorTextInput.setText(hexColor)
            setColors(color)
        })

        colorTextInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val content = colorTextInput.text
                val nums: String
                if (content.isEmpty()) {
                    colorTextInput.error = "Input value can't be empty"
                    return
                } else {
                    nums = content.substring(1)
                }

                var hex = true
                try {
                    parseInt(nums, 16)
                } catch (e: NumberFormatException) {
                    hex = false
                }

                colorTextInput.error =
                        if (content.startsWith("#") && content.length == 7 && hex) null
                        else "Input needs to be a valid hex code"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        var count = 0 // No idea why the alert shows twice so this is my quick probably horrible "fix" lmao
        colorTextInput.setOnKeyListener({ _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (colorTextInput.error != null) {
                    count++
                    if (count == 1) {
                        toast(colorTextInput.error).show()
                    } else {
                        count = 0
                    }
                    return@setOnKeyListener true
                }
                val color = Color.parseColor(colorTextInput.text.toString())
                setColors(color)
                return@setOnKeyListener true
            }
            false
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setColors(color: Int) {
        setStatusBarColor(color)
        imageView.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        colorPicker.setColor(color)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        colorTextInput.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        textViewRGB.text = "RGB: $r, $g, $b"

        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(r, g, b, hsl)
        textViewHSL.text = "HSL: ${round(hsl[0].toDouble())}, ${round(hsl[1].toDouble())}, ${round(hsl[2].toDouble())}"
    }

    private fun setStatusBarColor(color: Int) {
        if (color == Color.BLACK && this.window.navigationBarColor == Color.BLACK) {
            this.window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            this.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
        this.window.statusBarColor = darken(color, 0.2f)
    }

    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = darkenColor(Color.red(color), factor)
        val g = darkenColor(Color.green(color), factor)
        val b = darkenColor(Color.blue(color), factor)
        return Color.argb(a, r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Math.max(color - color * factor, 0.0f).toInt()
    }

    private fun round(num: Double): Double {
        return BigDecimal(num).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    }
}
