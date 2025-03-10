package com.example.leafhunterdevelopment.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.leafhunterdevelopment.R
import com.example.leafhunterdevelopment.databinding.FragmentSignUpBinding
import com.example.leafhunterdevelopment.ui.signup.SignUpViewModel

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // firebaseAuth = FirebaseAuth.getInstance()

        // Button to change to sign in
        binding.textView.setOnClickListener {
            // Navigate to SignInFragment
            findNavController().navigate(R.id.signInFragment)
        }

        // Sign up button listener
        binding.button.setOnClickListener {

            // Get inputs
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            // Check empty fields
            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                // Check password matching
                if (pass == confirmPass) {
                    // Commented out Firebase functions
                    // firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                    //     if (it.isSuccessful) {
                    //         val intent = Intent(activity, SignInActivity::class.java)
                    //         startActivity(intent)
                    //     } else {
                    //         Toast.makeText(context, it.exception.toString(), Toast.LENGTH_SHORT).show()
                    //     }
                    // }

                    // TODO: remove this and uncomment the above code
                    findNavController().navigate(R.id.navigationHome)
                } else {
                    Toast.makeText(context, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Empty fields aren't allowed.", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}