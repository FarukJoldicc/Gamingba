package com.faruk.gamingba.view.adapter

import android.util.Log
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

private const val TAG = "GameCarouselAdapter"

class GameCarouselAdapter(
    private val onGameClicked: (Game) -> Unit
) : ListAdapter<Game, GameCarouselAdapter.GameViewHolder>(GameDiffCallback()) {

    private var itemWidth: Int = ViewGroup.LayoutParams.MATCH_PARENT
    private val standardMargin = 16 // Standard gap between items

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
        
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position))
        
        // Apply margins based on position
        val layoutParams = holder.binding.root.layoutParams as MarginLayoutParams
        
        // Reset margins first
        layoutParams.leftMargin = 0
        layoutParams.rightMargin = 0
        
        when (position) {
            0 -> {
                // First item: no left margin, standard right margin
                layoutParams.rightMargin = standardMargin
            }
            itemCount - 1 -> {
                // Last item: standard left margin, no right margin
                layoutParams.leftMargin = standardMargin
            }
            else -> {
                // Middle items: standard left margin, standard right margin
                layoutParams.leftMargin = standardMargin
                layoutParams.rightMargin = standardMargin
            }
        }
        
        // Apply width
        layoutParams.width = itemWidth
        holder.binding.root.layoutParams = layoutParams
    }

    inner class GameViewHolder(
        val binding: ItemGameCarouselBinding
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