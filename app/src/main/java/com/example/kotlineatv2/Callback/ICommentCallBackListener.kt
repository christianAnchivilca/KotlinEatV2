package com.example.kotlineatv2.Callback

import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.CommentModel

interface ICommentCallBackListener {
    fun onCommentLoadSuccess(commentModelList:List<CommentModel>)
    fun onCommentLoadFailed(message:String)
}