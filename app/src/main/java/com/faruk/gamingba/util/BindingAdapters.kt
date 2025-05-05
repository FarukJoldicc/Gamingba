package com.faruk.gamingba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.faruk.gamingba.R
import com.faruk.gamingba.model.data.Game
import com.faruk.gamingba.view.adapter.GameCarouselAdapter

@BindingAdapter("bindText", "onTextChanged", requireAll = false)
fun bindTextWithCallback(editText: EditText, value: String?, onTextChanged: ((Any?) -> Unit)?) {
    if (editText.text.toString() != value) {
        editText.setText(value ?: "")
        value?.let { editText.setSelection(it.length) }
    }

    val tag = R.id.text_watcher_tag
    val oldWatcher = editText.getTag(tag) as? TextWatcher
    if (oldWatcher != null) {
        editText.removeTextChangedListener(oldWatcher)
    }

    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val newText = s.toString()
            if (newText != value) {
                onTextChanged?.invoke(newText)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    editText.addTextChangedListener(watcher)
    editText.setTag(tag, watcher)
}

@BindingAdapter("onTextChanged")
fun setOnTextChangedListener(editText: TextInputEditText, onTextChanged: ((Any?) -> Unit)?) {
    val tag = R.id.text_watcher_tag
    val oldWatcher = editText.getTag(tag) as? TextWatcher
    if (oldWatcher != null) {
        editText.removeTextChangedListener(oldWatcher)
    }

    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            onTextChanged?.invoke(s?.toString() ?: "")
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    editText.addTextChangedListener(watcher)
    editText.setTag(tag, watcher)
}

@BindingAdapter("imageTint")
fun setImageTint(imageView: ImageView, colorRes: Int) {
    val color = ContextCompat.getColor(imageView.context, colorRes)
    imageView.setColorFilter(color)
}

@BindingAdapter("imageUrl")
fun loadImage(imageView: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(imageView.context)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.placeholder_game)
            .error(R.drawable.placeholder_game)
            .into(imageView)
    }
}

@BindingAdapter("gamesList")
fun setGamesList(recyclerView: RecyclerView, games: List<Game>?) {
    games?.let {
        (recyclerView.adapter as? GameCarouselAdapter)?.submitList(it)
    }
}

@BindingAdapter("isVisible")
fun setVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("ratingText")
fun setRatingText(textView: TextView, rating: Float) {
    val formattedRating = String.format("%.1f", rating)
    textView.text = formattedRating
}

@BindingAdapter("releaseDateText")
fun setReleaseDateText(textView: TextView, dateString: String?) {
    if (dateString.isNullOrEmpty()) {
        textView.text = "TBA"
        return
    }
    
    try {
        // Input format is "YYYY-MM-DD"
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> parts[1]
            }
            val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
            
            // Format as "MMM DD, YYYY"
            textView.text = "$month $day, $year"
        } else {
            textView.text = dateString
        }
    } catch (e: Exception) {
        textView.text = dateString
    }
}
