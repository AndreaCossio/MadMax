package it.polito.mad.madmax.madmax

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_main.profile_image

class EditProfileActivity : AppCompatActivity() {
    private val REQUEST_IMAGE_CAPTURE = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        profile_edit_iv.setOnClickListener{dispatchTakePictureIntent()}
        //TODO get data from intent
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_save_profile, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", findViewById<TextInputEditText>(R.id.name_tiet).text.toString())
        outState.putString("nickname", findViewById<TextInputEditText>(R.id.nickname_tiet).text.toString())
        outState.putString("email", findViewById<TextInputEditText>(R.id.email_tiet).text.toString())
        outState.putString("location", findViewById<TextInputEditText>(R.id.location_tiet).text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        findViewById<TextInputEditText>(R.id.name_tiet).setText(savedInstanceState.getString("name"))
        findViewById<TextInputEditText>(R.id.nickname_tiet).setText(savedInstanceState.getString("nickname"))
        findViewById<TextInputEditText>(R.id.email_tiet).setText(savedInstanceState.getString("email"))
        findViewById<TextInputEditText>(R.id.location_tiet).setText(savedInstanceState.getString("location"))
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            profile_image.setImageBitmap(imageBitmap)
            profile_image.visibility= View.VISIBLE
            profile_edit_iv.visibility= View.INVISIBLE
        }
    }

}
