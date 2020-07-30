package com.example.kotlineatv2.ui.restaurant

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyCategoriesAdapter
import com.example.kotlineatv2.Adapter.MyRestaurantAdapter
import com.example.kotlineatv2.Common.SpaceItemDecoration
import com.example.kotlineatv2.R
import com.example.kotlineatv2.ui.menu.MenuViewModel
import dmax.dialog.SpotsDialog

class RestaurantFragment : Fragment() {


    private lateinit var viewModel: RestaurantViewModel

    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationControler: LayoutAnimationController
    private var restaurantAdapter: MyRestaurantAdapter?=null

    private var recycler_restaurant: RecyclerView?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RestaurantViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_restaurant, container, false)
        initView(root)

        viewModel.getMessageError().observe(this, Observer{
            Toast.makeText(context,it, Toast.LENGTH_LONG).show()
        })


        viewModel.getRestaurantList().observe(this, Observer {
            dialog.dismiss()
            restaurantAdapter = MyRestaurantAdapter(context!!,it)
            recycler_restaurant!!.adapter = restaurantAdapter
            recycler_restaurant!!.layoutAnimation = layoutAnimationControler
        })

        return root
    }

    private fun initView(root: View) {
        setHasOptionsMenu(true)

        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        dialog.show()
        layoutAnimationControler = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
        recycler_restaurant = root.findViewById(R.id.recycler_restaurant) as RecyclerView
        recycler_restaurant!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recycler_restaurant!!.layoutManager = layoutManager
        recycler_restaurant!!.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

    }

}