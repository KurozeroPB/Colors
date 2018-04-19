package xyz.kurozero.colors

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.support.v7.app.AppCompatActivity
import android.support.v4.graphics.ColorUtils
import android.support.design.widget.Snackbar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.icu.math.BigDecimal
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.provider.MediaStore
import android.view.*
import android.widget.Toast

import java.io.IOException
import java.util.Random
import java.lang.Integer.parseInt

import com.madrapps.pikolo.listeners.OnColorSelectionListener
import com.github.zawadz88.materialpopupmenu.popupMenu
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import butterknife.ButterKnife

/**
 * Main activity
 * @since 0.1.0
 */
class MainActivity : AppCompatActivity() {

    private val random = Random()
    private var menu: Menu? = null

    /**
     * @property [savedInstanceState] The application state?
     * @since 0.1.0
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        setSupportActionBar(toolbar)

        imageView.setOnLongClickListener({ view ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", colorTextInput.text)
            clipboard.primaryClip = clip
            Snackbar.make(view, "Saved ${colorTextInput.text} to clipboard", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return@setOnLongClickListener true
        })

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
                val item = menu!!.findItem(R.id.options_menu)
                item.icon.setColorFilter(getContrastColor(color), PorterDuff.Mode.MULTIPLY)
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
     * On menu create
     * @property [menu]
     * @since 0.3.1
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu

        return true
    }

    /**
     * On menu item selected
     * @property [item]
     * @since 0.3.1
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.options_menu) {
            val view = checkNotNull(findViewById<View>(R.id.options_menu))

            val popupMenu = popupMenu {
                section {
                    item {
                        label = "Select image"
                        icon = R.drawable.folder_multiple_image
                        callback = {
                            selectImageInAlbum()
                        }
                    }
                    item {
                        label = "Set background"
                        icon = R.drawable.cellphone_android
                        callback = {
                            setBackground(view)
                        }
                    }
                }
            }

            popupMenu.show(this@MainActivity, view)
            return true
        }
        return false
    }

    /**
     * If an image is selected set the colors to the dominant color from that image
     * @property [requestCode]
     * @property [resultCode]
     * @property [intent]
     * @since 0.3.0
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_CANCELED) return
        val contentURI = intent!!.data
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, contentURI)
            val color = getDominantColor(bitmap)
            val hexColor = String.format("#%06X", 0xFFFFFF and color)
            colorTextInput.setText(hexColor)
            setColors(color)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Get the dominant color from an image
     * @property [bitmap]
     * @since 0.3.0
     */
    private fun getDominantColor(bitmap: Bitmap): Int {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val color = newBitmap.getPixel(0, 0)
        newBitmap.recycle()
        return color
    }

    /**
     * Select an image from your gallery
     * @since 0.3.0
     */
    private fun selectImageInAlbum() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 1)
    }

    /**
     * Set the system/lockscreen background to the current color
     * @property [view]
     * @since 0.3.0
     */
    private fun setBackground(view: View) {
        if (colorTextInput.error != null) {
            Snackbar.make(view, colorTextInput.error, Snackbar.LENGTH_LONG).setAction("Error", null).show()
            return
        } else if (colorTextInput.text.isEmpty()) {
            Snackbar.make(view, "Select a color first", Snackbar.LENGTH_LONG).setAction("Error", null).show()
            return
        }

        val color = Color.parseColor(colorTextInput.text.toString())
        val bitmap = getColorImage(1920, 1080, color)

        val buttons = listOf("Background", "Lockscreen", "Both")
        val errorSnack = Snackbar.make(view, "Failed setting background", Snackbar.LENGTH_LONG).setAction("Error", null)
        val successSnack = Snackbar.make(view, "Success setting background", Snackbar.LENGTH_LONG).setAction("Error", null)
        selector(null, buttons, { _, i ->
            when (i) {
                0 -> {
                    try {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        successSnack.show()
                    } catch (e: IOException) {
                        errorSnack.show()
                    }
                }
                1 -> {
                    try {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        successSnack.setText("Success setting lockscreen").show()
                    } catch (e: IOException) {
                        errorSnack.setText("Failed setting lockscreen").show()
                    }
                }
                2 -> {
                    try {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        successSnack.setText("Success setting background and lockscreen").show()
                    } catch (e: IOException) {
                        errorSnack.setText("Failed setting background and lockscreen").show()
                    }
                }
                else -> return@selector
            }
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
        val item = menu!!.findItem(R.id.options_menu)
        item.icon.setColorFilter(getContrastColor(color), PorterDuff.Mode.MULTIPLY)
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

    /**
     * Create an image bitmap from a color value
     * @property [width] Image width
     * @property [height] Image height
     * @property [color] Color value
     * @return Image bitmap
     * @since 0.3.0
     */
    private fun getColorImage(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        canvas.drawRect(0F, 0F, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
