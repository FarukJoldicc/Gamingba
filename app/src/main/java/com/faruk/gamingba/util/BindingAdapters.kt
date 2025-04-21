package com.faruk.gamingba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText
import com.faruk.gamingba.R

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
