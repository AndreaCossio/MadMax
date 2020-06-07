package it.polito.mad.madmax.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.polito.mad.madmax.R
import it.polito.mad.madmax.data.viewmodel.UserViewModel
import it.polito.mad.madmax.displayMessage
import it.polito.mad.madmax.getFragmentSpaceSize
import it.polito.mad.madmax.hideProgress
import kotlinx.android.synthetic.main.user_rate_dialog.*

class UserRateDialog : DialogFragment() {

    // Models
    private val userVM: UserViewModel by activityViewModels()
    private lateinit var userListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userVM.clearOtherUserData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.apply {
            setLayout(getFragmentSpaceSize(requireContext()).x, getFragmentSpaceSize(requireContext()).y)
            setBackgroundDrawableResource(android.R.color.transparent)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return inflater.inflate(R.layout.user_rate_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userVM.getOtherUserData().observe(viewLifecycleOwner, Observer { user ->
            if (user.userId != "") {
                dialog_rate_user_name.text = "${user.name}'s rating"
                dialog_rate_user_photo?.post {
                    Picasso.get().load(Uri.parse(user.photo)).into(dialog_rate_user_photo, object : Callback {
                        override fun onSuccess() {
                            dialog_rate_user_photo?.translationY = 0F
                            hideProgress(requireActivity())
                        }

                        override fun onError(e: Exception?) {
                            dialog_rate_user_photo?.apply {
                                translationY = measuredHeight / 6F
                                setImageDrawable(requireContext().getDrawable(R.drawable.ic_profile))
                            }
                            hideProgress(requireActivity())
                        }
                    })
                }
            }
        })
        userListener = userVM.listenOtherUser(requireArguments().getString("userId")!!)
        dialog_rate_button.setOnClickListener {
            userVM.rateUser(
                requireContext(),
                userVM.getOtherUserData().value!!.userId,
                "${requireArguments().getString("itemId")!!}+/${dialog_rate_rating_bar.rating}+/${dialog_rate_comment.text}"
            ).addOnSuccessListener {
                displayMessage(requireContext(), "Successfully rated")
                dismiss()
            }.addOnFailureListener {
                displayMessage(requireContext(), "Failed to rate user")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::userListener.isInitialized) {
            userListener.remove()
        }
    }
}
