package com.example.kotlineatv2.ui.view_orders

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.Unbinder
import com.androidwidgets.formatedittext.widgets.FormatEditText
import com.example.kotlineatv2.Adapter.MyOrderAdapter
import com.example.kotlineatv2.Callback.ILoadOrderCallbackListener
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Common.SwipeToDeleteCallback
import com.example.kotlineatv2.EventBus.MenuItemBack
import com.example.kotlineatv2.Model.OrderModel
import com.example.kotlineatv2.Model.RefundRequestModel
import com.example.kotlineatv2.Model.ShippingOrderModel
import com.example.kotlineatv2.R
import com.example.kotlineatv2.TrackingOrderActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.layout_refund_request.*
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ViewOrdersFragment:Fragment(),ILoadOrderCallbackListener {

    //VARIABLES MIEMBROS
    private var viewOrderModel:ViewOrdersViewModel?=null
    private var unbinder:Unbinder?=null
    internal lateinit var dialog:AlertDialog
    internal lateinit var recycler_order:RecyclerView
    internal lateinit var listener:ILoadOrderCallbackListener


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewOrderModel = ViewModelProviders.of(this).get(ViewOrdersViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_view_orders, container, false)
        initView(root)
        loadOrderFromFirebase()

        viewOrderModel!!.mutableLiveDataOrderList.observe(this, Observer {
                Collections.reverse(it!!)
                val adapter = MyOrderAdapter(context!!,it!!.toMutableList())
                recycler_order.adapter=adapter
        })


        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<OrderModel>()
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REFERENCE)
            .orderByChild("userId")
            .equalTo(Common.currentUser!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {
                  for (orderSnapShot in p0.children){
                      var order = orderSnapShot.getValue<OrderModel>(OrderModel::class.java)
                      order!!.orderNumber = orderSnapShot.key
                      orderList.add(order!!)
                  }
                    listener.onLoadOrderSuccess(orderList)
                }

            })

    }

    private fun initView(root: View?) {
        listener = this
        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()
        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context!!)
        recycler_order.layoutManager=layoutManager
        recycler_order.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

        val swipeHandler = @SuppressLint("UseRequireInsteadOfGet")
        object : SwipeToDeleteCallback(context!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                dialogQuestion(viewHolder.adapterPosition)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recycler_order)


    }

    private fun dialogQuestion(adapterPosition: Int) {

        val dialogo = AlertDialog.Builder(context!!)
        dialogo.setTitle("Elegir una opcion")
        dialogo.setCancelable(false)
        val itemview = LayoutInflater.from(context).inflate(R.layout.layout_dialog_slide_order,null)
        dialogo.setView(itemview)
        val rdb_cancelar_orden = itemview.findViewById<View>(R.id.rdb_cancelar_orden)as RadioButton
        val rdb_traking_orden = itemview.findViewById<View>(R.id.rdb_traking_orden)as RadioButton

        dialogo.setNegativeButton("CANCELAR"){dialogInterface,_->dialogInterface.dismiss()
        }
        dialogo.setPositiveButton("ACEPTAR"){dialogInterface,_->
            if (rdb_cancelar_orden.isChecked){

                val orderModel = (recycler_order!!.adapter as MyOrderAdapter).getItemAtPosition(adapterPosition)
                if (orderModel.orderStatus == 0)
                {
                    if (orderModel.isCod)
                    {
                        //mostrar dialogo
                        val builder= AlertDialog.Builder(context!!)
                        builder.setTitle("Cancelar Orden")
                            .setMessage("¿Estas seguro de cancelar la order?")
                            .setCancelable(false)
                            .setPositiveButton("Si",{dialogInterface,_->
                                //dialogInterface.dismiss()
                                val update_data = HashMap<String,Any>()
                                update_data.put("orderStatus",-1)

                                FirebaseDatabase.getInstance().getReference(Common.ORDER_REFERENCE)
                                    .child(orderModel.orderNumber!!)
                                    .updateChildren(update_data)
                                    .addOnFailureListener{e->
                                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnSuccessListener {
                                        Toast.makeText(context,"Orden Cancelada",Toast.LENGTH_SHORT).show()
                                        loadOrderFromFirebase()
                                    }



                            })
                            .setNegativeButton("No",{dialogInterface,_->
                                dialogInterface.dismiss()
                                loadOrderFromFirebase()

                            })
                        val createDialog = builder.create()
                        createDialog.show()
                    }
                    else{

                        val view = LayoutInflater.from(context!!)
                            .inflate(R.layout.layout_refund_request,null)

                        val edt_name = view.findViewById<View>(R.id.edt_card_name) as EditText
                        val edt_card_number = view.findViewById<View>(R.id.edt_card_number) as FormatEditText
                        val edt_card_exp = view.findViewById<View>(R.id.edt_exp) as FormatEditText
                        //set format
                        edt_card_number.setFormat("---- ---- ---- ----")
                        edt_card_exp.setFormat("--/--")



                        val builder= AlertDialog.Builder(context!!)
                        builder.setTitle("Cancelar Orden")
                            .setMessage("¿Estas seguro de cancelar la order?")
                            .setCancelable(false)
                            .setPositiveButton("Si",{dialogInterface,_->


                                val refundRequestModel = RefundRequestModel()
                                refundRequestModel.name = Common.currentUser!!.name!!
                                refundRequestModel.phone = Common.currentUser!!.phone!!
                                refundRequestModel.cardNumber = edt_card_number.text.toString()
                                refundRequestModel.cardExp = edt_card_exp.text.toString()
                                refundRequestModel.amount = orderModel.finalpayment
                                refundRequestModel.cardName = edt_name.text.toString()



                                FirebaseDatabase.getInstance()
                                    .getReference(Common.REFUND_REQUEST_REF)
                                    .child(orderModel.orderNumber!!)
                                    .setValue(refundRequestModel)
                                    .addOnFailureListener{e->
                                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnSuccessListener {

                                        //update data firebase
                                        val update_data = HashMap<String,Any>()
                                        update_data.put("orderStatus",-1)

                                        FirebaseDatabase.getInstance().getReference(Common.ORDER_REFERENCE)
                                            .child(orderModel.orderNumber!!)
                                            .updateChildren(update_data)
                                            .addOnFailureListener{e->
                                                Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnSuccessListener {
                                                Toast.makeText(context,"Orden Cancelada",Toast.LENGTH_SHORT).show()
                                                loadOrderFromFirebase()
                                            }
                                    }



                            })
                            .setNegativeButton("No",{dialogInterface,_->
                                dialogInterface.dismiss()
                                loadOrderFromFirebase()

                            })
                        builder.setView(view)
                        val createDialog = builder.create()
                        createDialog.show()

                    }

                }else{
                    Toast.makeText(context,"El estado de la orden ha cambiado",Toast.LENGTH_SHORT).show()
                    loadOrderFromFirebase()
                }


            }else if(rdb_traking_orden.isChecked){

                val orderModel2 = (recycler_order!!.adapter as MyOrderAdapter).getItemAtPosition(adapterPosition)

                FirebaseDatabase.getInstance()
                    .getReference(Common.SHIPPING_ORDER_REF)
                    .child(orderModel2.orderNumber!!)
                    .addListenerForSingleValueEvent(object:ValueEventListener{
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context,""+error.message,Toast.LENGTH_SHORT).show()
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if (p0.exists()){
                                Common.currentShippingOrder = p0.getValue(ShippingOrderModel::class.java)
                                Common.currentShippingOrder!!.key = p0.key
                                if (Common.currentShippingOrder!!.currentLat!! != -1.0 &&
                                    Common.currentShippingOrder!!.currentLng!! != -1.0){

                                    startActivity(Intent(context!!, TrackingOrderActivity::class.java))



                                }else{
                                    Toast.makeText(context,"Su pedido no ha sido enviado,espere porfavor",Toast.LENGTH_SHORT).show()
                                }

                            }else{
                                Toast.makeText(context,"Aun no se envia tu pedido",Toast.LENGTH_SHORT).show()
                            }

                        }

                    })


            }

        }


        val crearDialog = dialogo.create()
        crearDialog.show()




    }

    override fun onLoadOrderSuccess(orderList: List<OrderModel>) {
        dialog.dismiss()
        viewOrderModel!!.setMutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
       Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }


}