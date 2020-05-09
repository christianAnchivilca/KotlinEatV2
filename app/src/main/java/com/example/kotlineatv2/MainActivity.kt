package com.example.kotlineatv2

import android.accounts.Account
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.example.kotlineatv2.Common.Common
import com.example.kotlineatv2.Model.UserModel
import com.example.kotlineatv2.Remote.ICloudFunctions
import com.example.kotlineatv2.Remote.RetrofitCloudClient
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.disposables.CompositeDisposable
import retrofit2.create
import java.util.*

class MainActivity : AppCompatActivity() {

    internal lateinit var  firebaseAuth:FirebaseAuth
    internal lateinit var  listener:FirebaseAuth.AuthStateListener
    internal lateinit var dialog:AlertDialog
    internal val compositeDisposable = CompositeDisposable()
    private lateinit var iCloudFunctions:ICloudFunctions
    private lateinit var userRef:DatabaseReference
    private var providers:List<AuthUI.IdpConfig>? = null


    companion object{
        private val APP_REQUEST_CODE = 878
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        super.onStop()
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init(){

        providers = Arrays.asList<AuthUI.IdpConfig>(AuthUI.IdpConfig.PhoneBuilder().build())

        //instancia a la data en firebase
        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()

        //SE MUESTRA UN DIALOGO
        dialog = SpotsDialog.Builder()
            .setContext(this)
            .setCancelable(false)
            .build()
        //iCloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)

        //AuthStateListener se llama cuando hay un cambio en el estado de autenticación.
        listener = FirebaseAuth.AuthStateListener {

            Dexter.withActivity(this@MainActivity)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object:PermissionListener{
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        //get user firebase
                        val user = firebaseAuth.currentUser
                        //CHEKEAMOS SI EXISTE EL USUARIO REGISTRADO EN FIREBASE
                        if (user != null)
                        {
                            //already login - ya inicio session
                            checkUserFromFirebase(user!!)

                            // Toast.makeText(baseContext,"Ya Inicio de nuevo!",Toast.LENGTH_LONG).show()
                        }else{

                            //METODO PARA VALIDAR NUMERO DE CELULAR
                            phoneLogin()
                        }


                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        Toast.makeText(this@MainActivity,"No aceptaste los permisos",Toast.LENGTH_LONG).show()
                    }

                }).check()

        }


    }

    private fun phoneLogin(){

        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers!!).build(),
            APP_REQUEST_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE){

            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }else{
                Toast.makeText(baseContext,"Fallo en inicio de sesion",Toast.LENGTH_LONG).show()
            }

        }
    }


    private fun checkUserFromFirebase(user: FirebaseUser){
        dialog!!.show()
        userRef!!.child(user!!.uid)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_LONG).show()
                }
                override fun onDataChange(dataLogin: DataSnapshot) {

                    //VALIDAMOS SI EL USUARIO YA ESTA REGISTRADO EN FIREBASE
                    if (dataLogin.exists()){
                        val userModel = dataLogin.getValue(UserModel::class.java)
                        goToHomeActivity(userModel)
                        Toast.makeText(baseContext,"Welcome back: "+userModel!!.name,Toast.LENGTH_LONG).show()

                    }else{
                        showRegisterDialog(user!!)

                    }

                    dialog!!.dismiss()
                }

            })
    }

    private fun showRegisterDialog(user: FirebaseUser) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registro")
        builder.setMessage("Completa tu informacion")

        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.layout_register,null)
        val edt_name = itemView.findViewById<EditText>(R.id.edt_name)
        val edt_address = itemView.findViewById<EditText>(R.id.edt_address)

        val edt_phone = itemView.findViewById<EditText>(R.id.edt_phone)

        //set
        edt_phone.setText(user!!.phoneNumber)


        //Aqui seteamos nuestro formulario de registro dentro del AlertDialogo
        builder.setView(itemView)

        builder.setNegativeButton("CANCEL"){dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("REGISTRAR"){dialogInterface, i ->
            if(TextUtils.isDigitsOnly(edt_name.text.toString()) ) {
                Toast.makeText(this@MainActivity,"Please enter your name",Toast.LENGTH_LONG).show()
                return@setPositiveButton
            }else if(TextUtils.isDigitsOnly(edt_address.text.toString())){
                Toast.makeText(this@MainActivity,"Please enter your address",Toast.LENGTH_LONG).show()
                return@setPositiveButton
            }


            val userModel = UserModel()
            userModel.uid = user!!.uid
            userModel.name = edt_name.text.toString()
            userModel.address = edt_address.text.toString()
            userModel.phone = edt_phone.text.toString()

            userRef!!.child(user!!.uid)
                .setValue(userModel)
                .addOnCompleteListener{
                    task ->
                    if (task.isSuccessful) {
                        dialogInterface.dismiss()
                        Toast.makeText(this@MainActivity,"Registrado con exito!",Toast.LENGTH_LONG).show()
                        goToHomeActivity(userModel)
                    }else{
                        Toast.makeText(this@MainActivity,"Error al momento de registrar.",Toast.LENGTH_LONG).show()
                    }
                }

        }

        val dialogo1 = builder.create()
        dialogo1.show()


    }

    private fun goToHomeActivity(userModel: UserModel?) {
        Common.currentUser = userModel!!
        startActivity(Intent(this@MainActivity,HomeActivity::class.java))
        finish()



    }

}
