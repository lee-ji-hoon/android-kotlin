package com.guide.room_with_view

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author jihoon
 * @email dlwlgns1240@gmail.com
 * @created 2022/09/14
 * @desc
 */

// 프로퍼티 (name,age 기본값 설정)
class UserViewModel(name: String = "", age: Int = 0) : ViewModel() {
    var name: MutableLiveData<String> = MutableLiveData(name)
    var age: MutableLiveData<Int> = MutableLiveData(age)

    fun minus() {
        Log.d("TAG", "$name | $age")
        age.value = age.value!! - 1
    }

    fun plus() {
        Log.d("TAG", "$name | $age")
        age.value = age.value!! + 1
    }
}