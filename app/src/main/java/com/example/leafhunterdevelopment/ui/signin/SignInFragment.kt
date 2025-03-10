package com.example.leafhunterdevelopment.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.leafhunterdevelopment.R
import com.example.leafhunterdevelopment.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignInViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(SignInViewModel::class.java)
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // firebaseAuth = FirebaseAuth.getInstance()

        // Button to change to sign up
        binding.textView.setOnClickListener {
            // Navigate to SignUpFragment
            findNavController().navigate(R.id.signUpFragment)
        }

        // Sign in button listener
        binding.button.setOnClickListener {

            // Get inputs
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                // firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                //     if (it.isSuccessful) {
                //         findNavController().navigate(R.id.navigationHome)
                //     } else {
                //         Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
                //     }
                // }


                // TODO: remove this and uncomment the above code
                findNavController().navigate(R.id.navigationHome)
            } else {
                Toast.makeText(context, "Empty fields aren't allowed", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in
        // if(firebaseAuth.currentUser != null){
        //     findNavController().navigate(R.id.navigationHome)
        // }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}