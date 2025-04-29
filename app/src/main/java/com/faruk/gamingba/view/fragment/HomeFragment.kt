package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.faruk.gamingba.databinding.FragmentHomeBinding
import com.faruk.gamingba.model.state.HomeViewState
import com.faruk.gamingba.viewmodel.HomeViewModel
import com.faruk.gamingba.viewmodel.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        
        // Set initial userName value
        binding.userName = navigationViewModel.userName.value
        
        // Observe NavigationViewModel's userName and update HomeViewModel
        lifecycleScope.launch {
            navigationViewModel.userName.collect { name ->
                viewModel.setUserName(name)
                binding.userName = name
            }
        }
        
        // Collect HomeViewModel state
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                // Update UI based on state
                if (state.isLoading) {
                    // Show loading indicator if needed
                } else if (state.error != null) {
                    // Handle error state
                }
                // Handle other state properties
            }
        }
        
        setUpListeners()
    }

    private fun setUpListeners() {
        // Implement any necessary listeners here
    }
}


