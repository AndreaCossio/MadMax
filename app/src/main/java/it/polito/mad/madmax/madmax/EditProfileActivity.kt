package it.polito.mad.madmax.madmax

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : AppCompatActivity() {

    private var imageBitmap= MutableLiveData<Bitmap>();
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        profile_edit_iv.setOnClickListener{dispatchTakePictureIntent()}
        profile_image.setOnClickListener{dispatchTakePictureIntent()}
        imageBitmap.observe(this, Observer{ profile_image.setImageBitmap(it)})
        //TODO get data from intent
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_save_profile, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", name_tiet.text.toString())
        outState.putString("nickname", nickname_tiet.text.toString())
        outState.putString("email", email_tiet.text.toString())
        outState.putString("location", location_tiet.text.toString())
        outState.putParcelable("bitmap", imageBitmap.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        name_tiet.setText(savedInstanceState.getString("name"))
        nickname_tiet.setText(savedInstanceState.getString("nickname"))
        email_tiet.setText(savedInstanceState.getString("email"))
        location_tiet.setText(savedInstanceState.getString("location"))
        imageBitmap.value= savedInstanceState.getParcelable("bitmap")
        if(imageBitmap.value!=null)
        {
            profile_image.visibility= View.VISIBLE
            profile_edit_iv.visibility= View.INVISIBLE
        }
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
            imageBitmap.value = data?.extras?.get("data") as Bitmap
            profile_image.setImageBitmap(imageBitmap.value)
            profile_image.visibility= View.VISIBLE
            profile_edit_iv.visibility= View.INVISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        intent = Intent(applicationContext, ShowProfileActivity::class.java)
        intent.putExtra("it.polito.mad.madmax.madmax.name", findViewById<TextInputEditText>(R.id.name_tiet).text.toString())
        intent.putExtra("it.polito.mad.madmax.madmax.nickname", findViewById<TextInputEditText>(R.id.nickname_tiet).text.toString())
        intent.putExtra("it.polito.mad.madmax.madmax.email", findViewById<TextInputEditText>(R.id.email_tiet).text.toString())
        intent.putExtra("it.polito.mad.madmax.madmax.location", findViewById<TextInputEditText>(R.id.location_tiet).text.toString())
        setResult(Activity.RESULT_OK, intent)
        finish()
        return true
    }

}
