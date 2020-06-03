package com.example.kotlineatv2.ui.foodlist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyFoodListAdapter
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.EventBus.MenuItemBack
import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.Model.FoodModel
import com.example.kotlineatv2.R
import org.greenrobot.eventbus.EventBus

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
            if (it != null){ //fix crash when category is empty foods
                adapter = MyFoodListAdapter(context!!,it)
                recycler_food_lis!!.adapter = adapter
                recycler_food_lis!!.layoutAnimation = layoutAnimator
            }



        })
        return root
    }

    private fun initView(root: View) {
        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar!!.setTitle(Common.category_selected!!.name)
        recycler_food_lis = root.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_lis!!.setHasFixedSize(true)
        recycler_food_lis!!.layoutManager = LinearLayoutManager(context)

        layoutAnimator = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu,menu)

        val menuItem = menu.findItem(R.id.action_search)
        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
        searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearchFoods(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
               return false
            }

        })

        //clear text when click to clear button close
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener{
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            //clear text
            ed.setText("")
            //clear query
            searchView.setQuery("",false)
            //collapse action view
            searchView.onActionViewCollapsed()
            //collapse the search widget
            menuItem.collapseActionView()
            //restore result to original
            foodListViewModel.getMutableFoodModelListData()

        }


    }

    private fun startSearchFoods(query: String?) {
        val resultFood = ArrayList<FoodModel>()
        for (i in 0 until Common.category_selected!!.foods!!.size){

            val foodModel = Common.category_selected!!.foods!![i]
            if (foodModel.name!!.toLowerCase().contains(query!!.toLowerCase())){
                //aquí guardaremos el  resultado de búsqueda
                resultFood.add(foodModel)
            }
        }
        foodListViewModel.getMutableFoodModelListData().value = resultFood


    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}