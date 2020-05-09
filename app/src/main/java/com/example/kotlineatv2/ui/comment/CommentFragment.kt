package com.example.kotlineatv2.ui.comment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyCommentAdapter
import com.example.kotlineatv2.Callback.ICommentCallBackListener
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.CommentModel
import com.example.kotlineatv2.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog

class CommentFragment : BottomSheetDialogFragment(),ICommentCallBackListener {

    private var commentViewModel:CommentViewModel?=null
    private var iCommentCallback:ICommentCallBackListener

    private var recycler_comment:RecyclerView?=null

    private var dialog:AlertDialog?=null

    init {
        iCommentCallback = this

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_comment_fragment,container,false)
        initView(itemView)

        loadCommentFromFirebase()
        commentViewModel!!.mutableLiveDataCommentList.observe(this, Observer {
             commentList ->
            val adapter = MyCommentAdapter(context!!,commentList)
            recycler_comment!!.adapter = adapter

        })

        return itemView
    }

    private fun loadCommentFromFirebase() {
        dialog!!.show()
        val commentModels = ArrayList<CommentModel>()
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REFERENCE)
            .child(Common.foodModelSelected!!.id!!)
            .orderByChild("commentTimeStamp")
            .limitToFirst(100)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    iCommentCallback.onCommentLoadFailed(p0.message!!)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (commentSnapShot in p0.children){
                        val commentModel = commentSnapShot.getValue<CommentModel>(CommentModel::class.java)
                        commentModels.add(commentModel!!)

                    }

                    iCommentCallback.onCommentLoadSuccess(commentModels)
                }

            })

    }

    private fun initView(itemView: View) {
        commentViewModel = ViewModelProviders.of(this).get(CommentViewModel::class.java)

        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        recycler_comment = itemView.findViewById(R.id.recycler_comment) as RecyclerView
        recycler_comment!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,true)
        recycler_comment!!.layoutManager = layoutManager
        recycler_comment!!.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

    }

    override fun onCommentLoadSuccess(commentModelList: List<CommentModel>) {
        dialog!!.dismiss()
        commentViewModel!!.setCommentList(commentModelList)

    }

    override fun onCommentLoadFailed(message: String) {

        Toast.makeText(context!!,""+message,Toast.LENGTH_LONG).show()
        dialog!!.dismiss()

    }

    companion object{
        private var instance : CommentFragment?=null
        fun getInstance():CommentFragment{
            if (instance == null)
                instance = CommentFragment()
            return instance!!
        }
    }


}