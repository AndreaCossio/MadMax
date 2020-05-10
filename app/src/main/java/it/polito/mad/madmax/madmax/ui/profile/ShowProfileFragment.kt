package it.polito.mad.madmax.madmax.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import it.polito.mad.madmax.madmax.R
import it.polito.mad.madmax.madmax.data.model.User
import it.polito.mad.madmax.madmax.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_show_profile.*

class ShowProfileFragment : Fragment() {

    // User

    // Destination arguments
    private val args: ShowProfileFragmentArgs by navArgs()

    // Listeners
    private lateinit var cardListener: View.OnLayoutChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        initListeners()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                findNavController().navigate(ShowProfileFragmentDirections.actionEditProfile())
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Update views using the local variable user
    private fun updateFields(user: User?) {
        // TODO show only non private data if other user
        user?.also {
            profile_name.text = it.name
            profile_nickname.text = it.nickname
            profile_email.text = it.email
            item_location.text = it.location
            profile_phone.text = it.phone
            if (it.photo != "") {
                profile_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(it.photo))!!)
            }
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            val cardView: CardView = v as CardView
            val imageView = (cardView.getChildAt(0) as ViewGroup).getChildAt(0)
            val user = args.user ?: User()

            // Radius of the card
            cardView.apply { radius = measuredHeight / 2F }

            // Translation of the photo
            imageView.apply {
                if (user?.photo == "") {
                    translationY = measuredHeight / 6F
                }
                /*translationY = if (user?.photo != "") {
                    0F
                } else {
                    v.measuredHeight / 6F
                }*/
            }

            // Visibility
            cardView.visibility = View.VISIBLE
        }
    }
}