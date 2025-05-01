package com.faruk.gamingba.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.faruk.gamingba.R
import com.faruk.gamingba.databinding.ItemGameCarouselBinding
import com.faruk.gamingba.model.data.Game

class GameCarouselAdapter(
    private val onGameClicked: (Game) -> Unit
) : ListAdapter<Game, GameCarouselAdapter.GameViewHolder>(GameDiffCallback()) {

    private var itemWidth: Int = ViewGroup.LayoutParams.MATCH_PARENT

    fun setItemWidth(width: Int) {
        this.itemWidth = width
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameCarouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        // Apply width to the root view
        val layoutParams = MarginLayoutParams(itemWidth, binding.root.layoutParams.height)
        val horizontalMargin = (parent.width - itemWidth) / 2
        if (horizontalMargin > 0) {
            // For first and last item, add extra margin for centering
            when (itemCount) {
                1 -> {
                    layoutParams.leftMargin = horizontalMargin
                    layoutParams.rightMargin = horizontalMargin
                }
            }
        }
        binding.root.layoutParams = layoutParams
        
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GameViewHolder(
        private val binding: ItemGameCarouselBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onGameClicked(getItem(position))
                }
            }
        }

        fun bind(game: Game) {
            binding.game = game
            binding.executePendingBindings()

            Glide.with(binding.gameImage.context)
                .load(game.backgroundImage)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_game)
                .error(R.drawable.placeholder_game)
                .into(binding.gameImage)
        }
    }

    class GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }
} 