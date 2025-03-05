package com.example.ccganandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject


// ItemData class - Holds the data for each item in the list
// This is generated from the response from the search call
class ItemData {
    var text: String? = null
    var jsonObject: JSONObject? = null
}

// Adapter to handle each item in the list
class BreakingBadAdapter(private val myDataset: MutableList<ItemData>, private val activity: MainActivity) :
        RecyclerView.Adapter<BreakingBadAdapter.MyViewHolder>(), View.OnClickListener {

    private val tag: String = this.javaClass.name

    // Handle onClick for list. Launches detail view
    override fun onClick(view: View?) {

        val textView = view?.findViewById<TextView>(R.id.itemtextview)
        val jsonObject: JSONObject =textView?.tag as JSONObject

        // Log selection
        Log.i(tag, "Clicked on " + textView.text)

        // Launch detail view
        val ni = Intent(activity, DetailViewActivity::class.java)
        ni.putExtra("itemData", jsonObject.toString())
        activity.startActivity(ni)

    }

    // Define basic view holder
    class MyViewHolder(val linearLayout: LinearLayout) : RecyclerView.ViewHolder(linearLayout)


    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("UseCompatLoadingForDrawables")
    @Suppress("Deprecation")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        // Get reference to TextView
        val textView = holder.itemView.findViewById<TextView>(R.id.itemtextview)
        textView.setCompoundDrawablePadding(20)

        // Set Text and data object (tag) for item
        textView.text = myDataset[position].text
        textView.tag = myDataset[position].jsonObject

        // Get Image from Cache, if available
        val drawableBitmap = MainActivity.imageCache[(textView.tag as JSONObject?)?.getString("img")]

        //textView.gravity = TextView.TEXT_ALIGNMENT_CENTER
        if (drawableBitmap != null) {
            textView.setCompoundDrawablesWithIntrinsicBounds(drawableBitmap, null, null, null)
        } else {
            val empty = BitmapDrawable()
            textView.setCompoundDrawablesWithIntrinsicBounds(
                empty,
                null,
                null,
                null
            )
        }

        // Set onClick listener
        holder.linearLayout.setOnClickListener(this)
    }

    // Return the size (item count) of the dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size

    // Create View Holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        // create a new view
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.search_result, parent, false) as LinearLayout

        return MyViewHolder(layout)
    }
}