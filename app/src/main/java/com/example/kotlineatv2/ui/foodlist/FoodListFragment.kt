package com.example.kotlineatv2.ui.foodlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyFoodListAdapter
import com.example.kotlineatv2.R

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel
    var recycler_food_lis : RecyclerView?=null
    var layoutAnimator:LayoutAnimationController?=null
    var adapter:MyFoodListAdapter?=null


    override fun onStop() {
        if (adapter != null)
            adapter!!.onStop()
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel = ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)

        initView(root)
        foodListViewModel.getMutableFoodModelListData().observe(this, Observer {


            adapter = MyFoodListAdapter(context!!,it)
            recycler_food_lis!!.adapter = adapter
            recycler_food_lis!!.layoutAnimation = layoutAnimator

        })
        return root
    }

    private fun initView(root: View) {
        recycler_food_lis = root.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_lis!!.setHasFixedSize(true)
        recycler_food_lis!!.layoutManager = LinearLayoutManager(context)

        layoutAnimator = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }
}