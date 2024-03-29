package com.malkinfo.answerandquestion

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.malkinfo.answerandquestion.databinding.ActivitySetupProfileBinding
import com.malkinfo.answerandquestion.model.User
import java.util.*


class SetupProfileActivity : AppCompatActivity() {
    var binding: ActivitySetupProfileBinding? = null
    var auth: FirebaseAuth? = null
    var database: FirebaseDatabase? = null
    var storage: FirebaseStorage? = null
    var selectedImage: Uri? = null
    var dialog: ProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        dialog = ProgressDialog(this)
        dialog!!.setMessage("Updating profile...")
        dialog!!.setCancelable(false)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        supportActionBar?.hide()
        binding!!.imageView.setOnClickListener(View.OnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 45)
        })
        binding!!.continueBtn02.setOnClickListener(View.OnClickListener {
            val name: String = binding!!.nameBox.getText().toString()
            if (name.isEmpty()) {
                binding!!.nameBox.setError("Please type a name")
                return@OnClickListener
            }
            dialog!!.show()
            if (selectedImage != null) {
                val reference = storage!!.reference.child("Profiles").child(
                        auth!!.uid!!
                )
                reference.putFile(selectedImage!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            val uid = auth!!.uid
                            val phone = auth!!.currentUser!!.phoneNumber
                            val name: String = binding!!.nameBox.getText().toString()
                            val user = User(uid, name, phone, imageUrl)
                            database!!.reference
                                    .child("users")
                                    .child(uid!!)
                                    .setValue(user)
                                    .addOnSuccessListener {
                                        dialog!!.dismiss()
                                        val intent = Intent(
                                                this@SetupProfileActivity,
                                                MainActivity::class.java
                                        )
                                        startActivity(intent)
                                        finish()
                                    }
                        }
                    }
                }
            } else {
                val uid = auth!!.uid
                val phone = auth!!.currentUser!!.phoneNumber
                val user = User(uid, name, phone, "No Image")
                database!!.reference
                        .child("users")
                        .child(uid!!)
                        .setValue(user)
                        .addOnSuccessListener {
                            dialog!!.dismiss()
                            val intent = Intent(this@SetupProfileActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            if (data.data != null) {
                val uri = data.data // filepath
                val storage = FirebaseStorage.getInstance()
                val time = Date().time
                val reference = storage.reference.child("Profiles").child(time.toString() + "")
                reference.putFile(uri!!).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val filePath = uri.toString()
                            val obj = HashMap<String, Any>()
                            obj["image"] = filePath
                            database!!.reference.child("users")
                                    .child(FirebaseAuth.getInstance().uid!!)
                                    .updateChildren(obj).addOnSuccessListener { }
                        }
                    }
                }
                binding!!.imageView.setImageURI(data.data)
                selectedImage = data.data
            }
        }
    }
}