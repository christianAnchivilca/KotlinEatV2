package com.example.kotlineatv2.ui.menu

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatv2.Adapter.MyCategoriesAdapter
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Common.SpaceItemDecoration
import com.example.kotlineatv2.EventBus.MenuItemBack
import com.example.kotlineatv2.Model.CategoryModel
import com.example.kotlineatv2.R
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.fragment_category.*
import org.greenrobot.eventbus.EventBus

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

        setHasOptionsMenu(true)


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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu,menu)

        val menuItem = menu.findItem(R.id.action_search)
        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))
        //event
        searchView.setOnQueryTextListener(object:androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearchMenu(query)
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
            menuViewModel.loadCategory()

        }



    }

    private fun startSearchMenu(query: String?) {
        val resultCategory = ArrayList<CategoryModel>()
        for (i in 0 until categoriesAdapter!!.getCategoryList().size){

            val categoryModel = categoriesAdapter!!.getCategoryList()[i]
            if (categoryModel.name!!.toLowerCase().contains(query!!.toLowerCase())){
                //aquí guardaremos el  resultado de búsqueda
                resultCategory.add(categoryModel)
            }
        }
        menuViewModel.getCategoryList().value = resultCategory


    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }
}