package com.example.ccganandroid


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.HashMap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale


class MainActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener{

    private val tag: String = this.javaClass.name

    private var seasons = arrayOf("1", "2", "3", "4", "5")
    private var seasonFilter = "1"
    var lastSearch = String()
    private var searchView : SearchView? = null
    private var sView: SearchView? = null

    private lateinit var recyclerView: RecyclerView
    private var viewAdapter: RecyclerView.Adapter<*>? = null
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var dataList: MutableList<ItemData> = emptyList<ItemData>().toMutableList()

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Search and Recycler views
        setupInterface()
    }

    // setupInterface
    private fun setupInterface() {

        // Get Reference to Recycler View
        viewManager = LinearLayoutManager(this)
        viewAdapter = BreakingBadAdapter(dataList, this)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            // All views are the same
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify a viewAdapter
            adapter = viewAdapter
        }

        // Get Reference to Search View
        sView = findViewById(R.id.searchView)!!
        // Set search hint
        sView!!.queryHint = """Search for Characters"""

        // Setup QueryTextListener
        sView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(this.javaClass.name, "onQueryTextSubmit was called")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(this.javaClass.name, "onQueryTextChange was called")
                lastSearch = newText!!
                doBBSearch(newText)
                return true
            }
        })

        // Setup season filter
        val spin: Spinner = findViewById(R.id.spinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasons)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spin.setAdapter(adapter)
        spin.onItemSelectedListener = this

        searchView = sView
    }



    // doBBSearch
    @SuppressLint("NotifyDataSetChanged")
    private fun  doBBSearch(searchTxt: String?)
    {
        val doBBSearchRunnable = Runnable {

            // Search string template
            // https://breakingbadapi.com/api/characters?name=
            val searchUrlString: String = String.format(
                "https://breakingbadapi.com/api/characters?name=%s",
                searchTxt
            )

            val url = URL(searchUrlString)
            // Create connection
            val con: HttpsURLConnection?
            try {
                con = url.openConnection() as HttpsURLConnection?
            } catch (e: IOException) {
                Log.d(this.javaClass.name, e.localizedMessage!!)
                e.printStackTrace()
                return@Runnable
            }

            // Set some parameters
            con!!.setUseCaches(false)
            con.setDoInput(true)
            con.setConnectTimeout(30 * 1000) // 30 seconds to connect
            con.setReadTimeout(50 * 1000) // 5 second timeout
            con.setRequestMethod("GET")

            try {
                con.connect()
                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(tag, con.responseMessage)
                    return@Runnable
                }
                val baResponse = readFully(con.inputStream)
                val size = baResponse?.size
                if (size!! > 0) {
                    val sTemp = String(baResponse, Charsets.UTF_8)
                    Log.d(tag, sTemp)

                    val resArray = JSONArray(sTemp)
                    val count = resArray.length()

                    // Reset data
                    dataList.clear()

                    // add changed data
                    for (index in 0 until count)
                    {
                        val item: JSONObject = resArray.get(index) as JSONObject

                       val seasonFilter : Int = seasonFilter.toInt()

                        val inSeason = containsSeason(seasonFilter, item)

                        if (inSeason) {
                            // Log Item details
                            Log.i(tag, "Name = " + item.getString("name"))
                            Log.i(tag, "Char ID = " + item.getString("char_id"))
                            Log.i(tag, "Birthday = " + item.getString("birthday"))
                            Log.i(tag, "Occupation = " + item.getString("occupation"))
                            Log.i(tag, "Image Link = " + item.getString("img"))
                            Log.i(tag, "Status = " + item.getString("status"))
                            Log.i(tag, "Nick Name = " + item.getString("nickname"))
                            Log.i(tag, "Portrayed = " + item.getString("portrayed"))
                            Log.i(tag, "Category = " + item.getString("category"))

                            val iData = ItemData()
                            iData.text = item.getString("name")
                            iData.jsonObject = item

                            dataList.add(iData)

                            // Load Image
                            cacheImage(item.getString("img"))
                        }
                    }
                    // Notify there is an update
                    runOnUiThread { viewAdapter!!.notifyDataSetChanged() }

                }
            } catch (e: Exception) {
                Log.d(tag, "Error: " + e.localizedMessage)
                if (e.javaClass.simpleName.equals("JSONException")){
                    runOnUiThread { viewAdapter!!.notifyDataSetChanged() }
                    // Notify there is an update
                }
                return@Runnable
            } finally {
                con.disconnect()
            }


        }

        // Run thread
        Executors.newSingleThreadExecutor().execute(doBBSearchRunnable)
    }

    private fun containsSeason(season: Int, jsonObject: JSONObject): Boolean {
        var contains = false

        val array : JSONArray= jsonObject.get("appearance") as JSONArray
        for (i in 0 until array.length()) {
            val seasonFromIndex = array.get(i)
            if (seasonFromIndex == season) {
                contains = true
            }
        }
        return contains
    }

    // CLass variable for Image cache
    companion object {
        var imageCache = HashMap<String, Drawable>()
    }

    // cacheImage
    @SuppressLint("NotifyDataSetChanged")
    private fun cacheImage(imageUrl: String?)
    {
        // No need to load, if we already have it
        if (imageCache.containsKey(imageUrl)){
            return
        }

        // Load it
        val doLoadImageRunnable = Runnable {

            val url = URL(imageUrl)

            // Create connection
            val con: HttpsURLConnection?
            try {
                con = url.openConnection() as HttpsURLConnection?
            } catch (e: IOException) {
                Log.d(this.javaClass.name, e.localizedMessage!!)
                e.printStackTrace()
                return@Runnable
            }

            // Set some parameters
            con!!.setUseCaches(false)
            con.setDoInput(true)
            con.setConnectTimeout(30 * 1000) // 30 seconds to connect
            con.setReadTimeout(50 * 1000) // 5 second timeout
            con.setRequestMethod("GET")

            try {
                con.connect()
                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(tag, con.responseMessage)
                    return@Runnable
                }
                val baResponse = readFully(con.inputStream)
                val size = baResponse?.size
                if (size!! > 0) {
                    // Create bitmap drawable for use in UI
                    val bmd = getDrawableFromData(baResponse)
                    imageCache[imageUrl!!] = bmd

                    runOnUiThread { viewAdapter!!.notifyDataSetChanged() }
                }
            } catch (e: Exception) {
                Log.d(tag, "Error: " + e.localizedMessage)
                return@Runnable
            } finally {
                con.disconnect()
            }
        }

        // Run thread
        Executors.newSingleThreadExecutor().execute(doLoadImageRunnable)
    }

    private fun getDrawableFromData(buffer: ByteArray ): BitmapDrawable {
        val b: Bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
        val bs: Bitmap = b.scale(200, 250, false)
        val bd = bs.toDrawable(resources)
        return bd
    }

    // routine to fully read response
    @Synchronized
    @Throws(IOException::class, OutOfMemoryError::class, IllegalArgumentException::class)
    fun readFully(`is`: InputStream?): ByteArray? {
        requireNotNull(`is`) { "input stream can not be null" }

        return try {
            val baf = ByteArrayOutputStream()
            val tmp = ByteArray(4096)
            var l: Int
            while (`is`.read(tmp).also { l = it } != -1) {
                baf.write(tmp, 0, l)
            }
            baf.toByteArray()
        } catch (e: java.lang.Exception) {
            Log.d(tag, "read error", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.d(tag, "memory error", e)
            null
        } finally {
            `is`.close()
        }
    }

    // onItemSelected
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        seasonFilter = seasons[position]

        Log.d(this.javaClass.name, "onQueryTextChange was called")
        doBBSearch(lastSearch)

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // TODO: Nothing yet implemented
    }
}

