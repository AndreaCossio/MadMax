package it.polito.mad.madmax.lab02.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import it.polito.mad.madmax.lab02.R
import it.polito.mad.madmax.lab02.data_models.User
import it.polito.mad.madmax.lab02.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_show_profile.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class ShowProfileFragment : Fragment() {

    // User
    private var user: User? = null

    // Destination arguments
    private val args: ShowProfileFragmentArgs by navArgs()

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initListeners()
        readUser()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFields()
        profile_card.addOnLayoutChangeListener(cardListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profile_card.removeOnLayoutChangeListener(cardListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_edit_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_profile_edit -> {
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile(user?.copy()))
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // TODO as IMPROVEMENT pass user as arg also from the home
    // Load user info from args or from shared pref
    private fun readUser() {
        args.user?.also {
            user = it
            updateDrawerInfo(it)
        } ?: activity?.getSharedPreferences(getString(R.string.preferences_user_file), Context.MODE_PRIVATE)?.getString(getString(R.string.preference_user), null)?.also {
            user = Gson().fromJson(it, User::class.java)
        }
    }

    // Update views using the local variable user
    private fun updateFields() {
        user?.also {
            profile_name.text = it.name
            profile_nickname.text = it.nickname
            profile_email.text = it.email
            item_location.text = it.location
            profile_phone.text = it.phone
            it.photo?.also { uri ->
                profile_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(uri))!!)
            }
        }
    }

    // Update drawer info
    private fun updateDrawerInfo(user: User) {
        activity?.nav_view?.getHeaderView(0)?.also { navView ->
            navView.nav_header_nickname.text = user.name
            navView.nav_header_email.text = user.email
            user.photo?.also { photo ->
                navView.nav_header_profile_photo.apply {
                    translationY = 0F
                    setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(photo))!!)
                }
            }
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            (v as CardView).apply {
                radius = measuredHeight / 2F
            }
            v.visibility = View.VISIBLE
            val photoView = (v.getChildAt(0) as ViewGroup).getChildAt(0)
            user?.photo.also {
                photoView.apply {
                    translationY = 0F
                }
            } ?: photoView.apply {
                translationY = v.measuredHeight / 6F
            }
        }
    }

}