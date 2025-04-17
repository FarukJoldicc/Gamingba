package com.faruk.gamingba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.databinding.BindingAdapter
import kotlinx.coroutines.flow.MutableStateFlow

@BindingAdapter("bindText")
fun bindText(editText: EditText, stateFlow: MutableStateFlow<String>) {
    if (editText.text.toString() != stateFlow.value) {
        editText.setText(stateFlow.value)
    }

    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (stateFlow.value != s.toString()) {
                stateFlow.value = s.toString()
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
