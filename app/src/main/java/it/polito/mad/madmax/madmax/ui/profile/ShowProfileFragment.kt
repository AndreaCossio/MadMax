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
import it.polito.mad.madmax.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.madmax.handleSamplingAndRotationBitmap
import kotlinx.android.synthetic.main.fragment_show_profile.*

// TODO bug rotating delete info user
class ShowProfileFragment : Fragment() {

    // User
    private val userVM: UserViewModel by activityViewModels()

    // Destination arguments
    private val args: ShowProfileFragmentArgs by navArgs()
    private var otherUser: Boolean = false

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
        args.user?.also {
            otherUser = true
            updateFields(it)
        } ?: userVM.user.observe(viewLifecycleOwner, Observer { updateFields(it) })
        profile_card.addOnLayoutChangeListener(cardListener)
    }

    override fun onDestroyView() {
        profile_card.removeOnLayoutChangeListener(cardListener)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!otherUser) {
            inflater.inflate(R.menu.menu_edit_profile, menu)
        }
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
    private fun updateFields(user: User) {
        profile_name.text = user.name
        profile_nickname.text = user.nickname
        profile_email.text = user.email
        profile_location.text = user.location
        profile_phone.text = user.phone
        if (user.photo != "") {
            profile_photo.setImageBitmap(handleSamplingAndRotationBitmap(requireContext(), Uri.parse(user.photo))!!)
        }

        if (otherUser) {
            profile_location.visibility = View.GONE
            profile_phone.visibility = View.GONE
        }
    }

    // Initialize listeners
    private fun initListeners() {
        // This listener is necessary to make sure that the cardView has always 50% radius (circle)
        // and that if the image is the icon, it is translated down
        cardListener = View.OnLayoutChangeListener {v, _, _, _, _, _, _, _, _ ->
            val cardView: CardView = v as CardView
            val imageView = (cardView.getChildAt(0) as ViewGroup).getChildAt(0)
            val user = args.user ?: userVM.user.value

            // Radius of the card
            cardView.apply { radius = measuredHeight / 2F }

            // Translation of the photo
            imageView.apply {
                translationY = if (user?.photo == "") {
                    measuredHeight / 6F
                } else {
                    0F
                }
            }

            // Visibility
            cardView.visibility = View.VISIBLE
        }
    }
}