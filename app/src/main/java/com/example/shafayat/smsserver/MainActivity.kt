package com.example.shafayat.smsserver

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.READ_SMS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.stfalcon.smsverifycatcher.OnSmsCatchListener
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher
import android.provider.Telephony
import android.content.ContentResolver
import android.util.Log
import android.view.View
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import retrofit2.Call
import retrofit2.Callback
import java.io.File
import java.io.FileOutputStream
import java.lang.Long
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE: Int = 9876
    lateinit var smsVerifyCatcher: SmsVerifyCatcher;
    var donors: MutableList<Donor> = arrayListOf()
    lateinit var group : String
    lateinit var area : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!isSmsPermissionGranted()) {
            requestReadSmsPermission()
        }

        smsVerifyCatcher = SmsVerifyCatcher(this, object : OnSmsCatchListener<String> {
            override fun onSmsCatch(message: String) {
                if (message.indexOf("donor") == 0) {
                    var name = message.subSequence(message.indexOf("name") + 5, message.indexOf("contact_no") - 1).toString()
                    var contact_no = message.subSequence(message.indexOf("contact_no") + 11, message.indexOf("group") - 1).toString()
                    var group = message.subSequence(message.indexOf("group") + 6, message.indexOf("area") - 1).toString()
                    var area = message.subSequence(message.indexOf("area") + 5, message.length - 1).toString()
                    saveToDonorList(name, contact_no, group, area)
                }
                if (message.indexOf("recipient") == 0) {
                    var name = message.subSequence(message.indexOf("name") + 5, message.indexOf("contact_no") - 1).toString()
                    var contact_no = message.subSequence(message.indexOf("contact_no") + 11, message.indexOf("group") - 1).toString()
                    group = message.subSequence(message.indexOf("group") + 6, message.indexOf("area") - 1).toString()
                    group = group.replace("(", "").replace(")", "")
                    group = group.replace("pos", "+").replace("neg", "-")

                    area = message.subSequence(message.indexOf("area") + 5, message.indexOf("hospital") + 9).toString()
                    var hospital = message.subSequence(message.indexOf("hospital") + 5, message.length - 1).toString()
                    getDonorList(group, area)

                }

            }
        })
    }

    private fun getDonorList(group: String, area: String) {
        var database = FirebaseFirestore.getInstance()
        var firebaseData: CollectionReference = database.collection("donors")

        firebaseData
                .get()
                .addOnCompleteListener(OnCompleteListener<QuerySnapshot> { task ->
                    if (task.isSuccessful) {
                        for (document in task.result) {
                            val data = document.data
                            if (data.get("group").toString().toLowerCase().equals(group.toLowerCase())) {
                                donors.add(Donor(data.get("name").toString(),
                                        data.get("contact_no").toString(),
                                        data.get("group").toString(),
                                        data.get("area").toString()))
                            }
                        }
                        if (donors.size > 0) {
                            sendResponse(this@MainActivity)
                        }
                    } else {

                    }
                }).addOnFailureListener(OnFailureListener {
                    Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
                })
    }

    private fun saveToDonorList(name: String, contact_no: String, group: String, area: String) {
        val donor = Donor(name = name,
                contact_no = contact_no,
                group = group,
                area = area)

        var database = FirebaseFirestore.getInstance()
        var firebaseData: CollectionReference = database.collection("donors")
        firebaseData.add(donor).addOnSuccessListener { documentReference ->
            run {
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            run {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        smsVerifyCatcher.onStart()
    }

    fun isSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }


    private fun requestReadSmsPermission() {

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), SMS_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        smsVerifyCatcher.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    fun sendResponse(context: Context) {

        val cr = context.contentResolver
        val c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
        var totalSMS = 0
        if (c != null) {
            totalSMS = c.count
            if (c.moveToFirst()) {
                var message = ""
                var number = c.getString(2).toString()
                var i = 0
                for (donor in donors) {
                    if(i<2) {
                        message += donor.name + "-" + donor.contact_no + ","
                    }else{
                        break
                    }
                    i++
                }

                val thread = Thread(Runnable {
                    try {
                        val link = "http://sms.greenweb.com.bd/api.php?token=11fa1f9d0664dbc9e11a0f6e7d8acc4d&to="+ number +"&message=" + message

                        val input = URL(link).openStream()
                        val output = FileOutputStream(File("response.html"))
                        input.use { _ ->
                            output.use { _ ->
                                input.copyTo(output)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("Error: ", e.message)
                    }
                })

                thread.start()


                c.close()

            } else {
                Toast.makeText(this, "No message to show!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun send(view : View){
        val cr = view.context.contentResolver
        val c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
        var totalSMS = 0
        if (c != null) {
            totalSMS = c.count
            if (c.moveToFirst()) {
                var message = c.getString(5).toString()
                if (message.indexOf("recipient") == 0) {
                    var name = message.subSequence(message.indexOf("name") + 5, message.indexOf("contact_no") - 1).toString()
                    var contact_no = message.subSequence(message.indexOf("contact_no") + 11, message.indexOf("group") - 1).toString()
                    group = message.subSequence(message.indexOf("group") + 6, message.indexOf("area") - 1).toString()
                    group = group.replace("(", "").replace(")", "")
                    group = group.replace("pos", "+").replace("neg", "-")

                    area = message.subSequence(message.indexOf("area") + 5, message.indexOf("hospital") + 9).toString()
                    var hospital = message.subSequence(message.indexOf("hospital") + 5, message.length - 1).toString()
                    getDonorList(group, area)

                }

                c.close()

            } else {
                Toast.makeText(this, "No message to show!", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
