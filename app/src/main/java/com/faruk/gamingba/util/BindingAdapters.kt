package com.faruk.gamingba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.databinding.BindingAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import com.faruk.gamingba.R

@BindingAdapter("bindText")
fun bindText(editText: EditText, stateFlow: MutableStateFlow<String>) {
    val existingWatcher = editText.getTag(R.id.text_watcher_tag) as? TextWatcher
    if (existingWatcher != null) {
        editText.removeTextChangedListener(existingWatcher)
    }

    if (editText.text.toString() != stateFlow.value) {
        editText.setText(stateFlow.value)
    }

    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (stateFlow.value != s.toString()) {
                stateFlow.value = s.toString()
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    editText.addTextChangedListener(watcher)
    editText.setTag(R.id.text_watcher_tag, watcher)
}

