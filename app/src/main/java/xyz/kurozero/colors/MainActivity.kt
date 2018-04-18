package xyz.kurozero.colors

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.support.v4.graphics.ColorUtils
import android.support.design.widget.Snackbar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.view.KeyEvent
import android.graphics.PorterDuff
import android.icu.math.BigDecimal

import java.util.Random
import java.lang.Integer.parseInt

import com.madrapps.pikolo.listeners.OnColorSelectionListener
import kotlinx.android.synthetic.main.activity_main.*
// import org.jetbrains.anko.toast

/**
 * Main activity
 * @since 0.1.0
 */
class MainActivity : AppCompatActivity() {

    private val random = Random()

    /**
     * @property [savedInstanceState] The application state?
     * @since 0.1.0
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        randomColorButton.setOnClickListener({
            val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
            val hexColor = String.format("#%06X", 0xFFFFFF and color)
            colorTextInput.setText(hexColor)
            setColors(color)
        })

        colorPicker.setColorSelectionListener(object : OnColorSelectionListener {
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
                toolbar.setBackgroundColor(color)
                toolbar.setTitleTextColor(getContrastColor(color))
            }

            override fun onColorSelectionStart(color: Int) {}
            override fun onColorSelectionEnd(color: Int) {}
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

        colorTextInput.setOnKeyListener({ view, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (colorTextInput.error != null) {
                    Snackbar.make(view, colorTextInput.error, Snackbar.LENGTH_LONG).setAction("Error", null).show()
                    return@setOnKeyListener true
                }
                val color = Color.parseColor(colorTextInput.text.toString())
                setColors(color)
                return@setOnKeyListener true
            }
            false
        })
    }

    /**
     * Function to quickly change the color of a bunch of widgets
     * @property [color] The color to set
     * @since 0.2.0
     */
    @SuppressLint("SetTextI18n")
    private fun setColors(color: Int) {
        setStatusBarColor(color)
        imageView.background.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        colorPicker.setColor(color)
        toolbar.setBackgroundColor(color)
        toolbar.setTitleTextColor(getContrastColor(color))
        colorTextInput.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        textViewRGB.text = "RGB: $r, $g, $b"

        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(r, g, b, hsl)
        textViewHSL.text = "HSL: ${round(hsl[0].toDouble())}, ${round(hsl[1].toDouble())}, ${round(hsl[2].toDouble())}"
    }

    /**
     * Darken an argb color value
     * @property [color] The color to darken
     * @property [factor] The factor to how much to darken by
     * @return The darkened color int
     * @since 0.2.0
     */
    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = darkenColor(Color.red(color), factor)
        val g = darkenColor(Color.green(color), factor)
        val b = darkenColor(Color.blue(color), factor)
        return Color.argb(a, r, g, b)
    }

    /**
     * Darken a specific color value
     * @property [color] The color to darken
     * @property [factor] The factor to how much to darken by
     * @return The darkened color int
     * @since 0.2.0
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        return Math.max(color - color * factor, 0.0f).toInt()
    }

    /**
     * Sets the status bar color
     * @property [color] The color to set the status bar as
     * @since 0.2.0
     */
    fun setStatusBarColor(color: Int) {
        if (color == Color.BLACK && this.window.navigationBarColor == Color.BLACK) {
            this.window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            this.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
        this.window.statusBarColor = darken(color, 0.2f)
    }

    /**
     * Rounds a Double to 2 decimals
     * @property [num] The value to round
     * @return The rounded value
     * @since 0.2.1
     */
    fun round(num: Double): Double {
        return BigDecimal(num).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
    }

    /**
     * Get the contrast and returns the appropriate color
     * @property [color] Color int to get the contrast from
     * @return Either black or white depending on the contrast
     * @since 0.2.2
     */
    fun getContrastColor(color: Int): Int {
        val a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }
}
