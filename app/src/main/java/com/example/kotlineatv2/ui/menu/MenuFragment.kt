package com.example.kotlineatv2.ui.menu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyCategoriesAdapter
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Common.SpaceItemDecoration
import com.example.kotlineatv2.R
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.fragment_category.*

class MenuFragment : Fragment() {

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var dialog:AlertDialog
    private lateinit var layoutAnimationControler:LayoutAnimationController
    private var categoriesAdapter:MyCategoriesAdapter?=null

    private var recycler_menu:RecyclerView?=null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        menuViewModel = ViewModelProviders.of(this).get(MenuViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_category, container, false)

        initView(root)

        menuViewModel.getMessageError().observe(this,Observer{
            Toast.makeText(context,it,Toast.LENGTH_LONG).show()
        })


        menuViewModel.getCategoryList().observe(this, Observer {
            dialog.dismiss()
            categoriesAdapter = MyCategoriesAdapter(context!!,it)
            recycler_menu!!.adapter = categoriesAdapter
            recycler_menu!!.layoutAnimation = layoutAnimationControler
        })

        return root
    }

    private fun initView(root:View) {
       dialog = SpotsDialog.Builder().setContext(context)
           .setCancelable(false).build()

        dialog.show()
        layoutAnimationControler = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        recycler_menu = root.findViewById(R.id.recycler_menu) as RecyclerView
        recycler_menu!!.setHasFixedSize(true)

        val layoutManager = GridLayoutManager(context,2)
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {

            override fun getSpanSize(position: Int): Int {
                return if (categoriesAdapter != null)
                {
                    when(categoriesAdapter!!.getItemViewType(position)){
                        Common.DEFAULT_COLUMN_COUNT -> 1
                        Common.FULL_WIDTH_COLUMN -> 2
                        else -> -1
                    }
                }else
                    -1
            }
        }

        recycler_menu!!.layoutManager = layoutManager
        recycler_menu!!.addItemDecoration(SpaceItemDecoration(8))

    }
}