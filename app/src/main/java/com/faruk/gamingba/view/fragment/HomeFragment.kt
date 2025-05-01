package com.faruk.gamingba.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.FragmentHomeBinding
import com.faruk.gamingba.model.state.HomeViewState
import com.faruk.gamingba.view.adapter.GameCarouselAdapter
import com.faruk.gamingba.viewmodel.GameViewModel
import com.faruk.gamingba.viewmodel.HomeViewModel
import com.faruk.gamingba.viewmodel.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "HomeFragment"
private const val MAX_INDICATOR_COUNT = 10

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    
    private lateinit var gameAdapter: GameCarouselAdapter
    private lateinit var snapHelper: PagerSnapHelper
    private var indicatorViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        
        setupBinding()
        setupRecyclerView()
        observeViewModels()
        
        // Explicitly call loadGames to ensure data is loaded
        gameViewModel.loadGames()
        
        setUpListeners()
    }
    
    private fun setupBinding() {
        Log.d(TAG, "setupBinding called")
        binding.apply {
            viewModel = this@HomeFragment.viewModel
            gameViewModel = this@HomeFragment.gameViewModel
            lifecycleOwner = viewLifecycleOwner
            userName = navigationViewModel.userName.value
        }
    }
    
    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView called")
        gameAdapter = GameCarouselAdapter { game ->
            // Handle game click - navigate to details
            // findNavController().navigate(HomeFragmentDirections.actionHomeToGameDetails(game.id))
            
            Log.d(TAG, "Game clicked: ${game.name} (id: ${game.id})")
            gameViewModel.loadGameDetails(game.id)
        }
        
        binding.gamesRecyclerView.adapter = gameAdapter
        
        // Set fixed item width for better display (approximately 90% of screen width)
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = (screenWidth * 0.85).toInt()
        gameAdapter.setItemWidth(itemWidth)
        
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.gamesRecyclerView.layoutManager = layoutManager
        
        // Snap helper for paging effect
        snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.gamesRecyclerView)
        
        // Scroll listener to update indicators
        binding.gamesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        gameViewModel.setCurrentPosition(position)
                        updateIndicators(position)
                    }
                }
            }
        })
        
        // Debug the adapter and recyclerview
        Log.d(TAG, "RecyclerView adapter set: ${binding.gamesRecyclerView.adapter != null}")
        Log.d(TAG, "RecyclerView layout manager: ${binding.gamesRecyclerView.layoutManager}")
    }
    
    private fun setupIndicators(count: Int) {
        val container = binding.carouselIndicator
        container.removeAllViews()
        indicatorViews.clear()
        
        for (i in 0 until count) {
            val view = if (i == 0) {
                // First indicator is active by default
                layoutInflater.inflate(R.layout.item_indicator_bar, container, false)
            } else {
                layoutInflater.inflate(R.layout.item_indicator_dot, container, false)
            }
            container.addView(view)
            indicatorViews.add(view)
        }
    }
    
    private fun updateIndicators(activePosition: Int) {
        for (i in indicatorViews.indices) {
            val view = indicatorViews[i]
            val container = binding.carouselIndicator
            
            // Remove the current view
            container.removeView(view)
            
            // Create a new view of the appropriate type
            val newView = if (i == activePosition) {
                layoutInflater.inflate(R.layout.item_indicator_bar, container, false)
            } else {
                layoutInflater.inflate(R.layout.item_indicator_dot, container, false)
            }
            
            // Add the new view at the same position
            container.addView(newView, i)
            
            // Update our reference
            indicatorViews[i] = newView
        }
    }
    
    private fun observeViewModels() {
        Log.d(TAG, "observeViewModels called")
        
        // Observe NavigationViewModel's userName and update HomeViewModel
        lifecycleScope.launch {
            navigationViewModel.userName.collect { name ->
                Log.d(TAG, "userName updated: $name")
                viewModel.setUserName(name)
                binding.userName = name
            }
        }
        
        // Collect HomeViewModel state
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                Log.d(TAG, "HomeViewModel state updated: $state")
                // Update UI based on state
                if (state.isLoading) {
                    // Show loading indicator if needed
                } else if (state.error != null) {
                    // Handle error state
                }
                // Handle other state properties
            }
        }
        
        // Collect GameViewModel carousel state
        lifecycleScope.launch {
            gameViewModel.carouselState.collect { state ->
                Log.d(TAG, "GameViewModel carouselState updated: games size=${state.games.size}, isLoading=${state.isLoading}, error=${state.error}")
                
                // Force the recyclerView adapter to update
                if (state.games.isNotEmpty()) {
                    // Use the games list directly without infinite loop
                    gameAdapter.submitList(state.games)
                    
                    // Set up the indicators
                    setupIndicators(state.games.size.coerceAtMost(MAX_INDICATOR_COUNT))
                    
                    // Set the carousel to start at the first position
                    binding.gamesRecyclerView.scrollToPosition(0)
                    updateIndicators(0)
                }
                
                // Make sure error text is visible if there's an error
                if (state.error != null) {
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.text = state.error
                } else {
                    binding.errorText.visibility = View.GONE
                }
            }
        }
        
        // Observe current position
        lifecycleScope.launch {
            gameViewModel.currentPosition.collect { position ->
                Log.d(TAG, "Current position updated: $position")
            }
        }
    }

    private fun setUpListeners() {
        // Implement any necessary listeners here
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }
}


