package com.example.ccganandroid


import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(),  AdapterView.OnItemSelectedListener{

    val TAG: String = this.javaClass.name;

    var seasons = arrayOf("1", "2", "3", "4", "5")
    var seasonFilter = "1"
    var lastSearch = String()
    var searchView : SearchView? = null
    var sview: SearchView? = null;

    private lateinit var recylerView: RecyclerView
    public var viewAdapter: RecyclerView.Adapter<*>? = null
        get() = field
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var datalist: MutableList<itemData> = emptyList<itemData>().toMutableList()

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.ccganandroid.R.layout.activity_main)

        // Setup Search and Recyler views
        setupInterface()
    }

    // onDestroy
    override fun onDestroy() {
        super.onDestroy()
    }

    // setupInterface
    fun setupInterface() {

        // Get Reference to Recycler View
        viewManager = LinearLayoutManager(this)
        viewAdapter = BreakingBadAdapter(datalist, this)

        recylerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            // All views are the same
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify a viewAdapter
            adapter = viewAdapter
        }

        // Get Reference to Search View
        sview = findViewById(R.id.searchView) as SearchView
        // Set search hint
        sview!!.queryHint = """Search for Characters"""

        // Setup QueryTextListener
        sview!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        val spin = findViewById(R.id.spinner) as Spinner
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, seasons)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spin.setAdapter(adapter);
        spin.setOnItemSelectedListener(this);

        searchView = sview
    }



    // doBBSearch
    private fun  doBBSearch(searchTxt: String?)
    {
        val doBBSearchRunnable: Runnable = Runnable {

            // Search string template
            // https://breakingbadapi.com/api/characters?name=
            val searchUrlString: String = String.format(
                "https://breakingbadapi.com/api/characters?name=%s",
                searchTxt
            )

            val url: URL = URL(searchUrlString)
            // Create connection
            val con: HttpsURLConnection?
            try {
                con = url.openConnection() as HttpsURLConnection?
            } catch (e: IOException) {
                Log.d(this.javaClass.name, e.localizedMessage!!)
                e.printStackTrace()
                return@Runnable
            }

            // Set some paramaters
            con!!.setUseCaches(false)
            con.setDoInput(true)
            con.setConnectTimeout(30 * 1000) // 30 seconds to connect
            con.setReadTimeout(50 * 1000) // 5 seond timeout
            con.setRequestMethod("GET")

            try {
                con.connect()
                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, con.responseMessage)
                    return@Runnable
                }
                val baResponse = readFully(con.inputStream)
                val size = baResponse?.size
                if (size!! > 0) {
                    val stemp: String = String(baResponse, Charsets.UTF_8)
                    Log.d(TAG, stemp)

                    val resArray = JSONArray(stemp)
                    val count = resArray.length()

                    // Reset data
                    datalist.clear();

                    // add changed data
                    for (index in 0..count-1)
                    {
                        val item: JSONObject = resArray.get(index) as JSONObject

                       val seasonFilter : Int = seasonFilter.toInt()

                        val inSeason = containsSeason(seasonFilter, item)

                        if (inSeason) {
                            // Log Item details
                            Log.i(TAG, "Name = " + item.getString("name"))
                            Log.i(TAG, "Char ID = " + item.getString("char_id"))
                            Log.i(TAG, "Birthday = " + item.getString("birthday"))
                            Log.i(TAG, "Occupation = " + item.getString("occupation"))
                            Log.i(TAG, "Image Link = " + item.getString("img"))
                            Log.i(TAG, "Status = " + item.getString("status"))
                            Log.i(TAG, "Nick Name = " + item.getString("nickname"))
                            Log.i(TAG, "Portrayed = " + item.getString("portrayed"))
                            Log.i(TAG, "Category = " + item.getString("category"))

                            val idata: itemData = itemData()
                            idata.text = item.getString("name")
                            idata.jsonObject = item

                            datalist.add(idata)

                            // Load Image
                            cacheImage(item.getString("img"))
                        }
                    }
                    // Notify there is an update
                    runOnUiThread(Runnable { viewAdapter!!.notifyDataSetChanged() })

                }
            } catch (e: Exception) {
                Log.d(TAG, "Error: " + e.localizedMessage)
                if (e.javaClass.simpleName.equals("JSONException")){
                    runOnUiThread(Runnable { viewAdapter!!.notifyDataSetChanged() })
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

    fun containsSeason(season: Int, jsonObject: JSONObject): Boolean {
        var contains = false;

        val array : JSONArray= jsonObject.get("appearance") as JSONArray
        for (i in 0 until array.length()) {
            val seasonFromIndex = array.get(i)
            if (seasonFromIndex == season) {
                contains = true;
            }
        }
        return contains
    }

    // CLass variable for Image cache
    companion object {
        var imageCache = HashMap<String, Drawable>()
    }

    // cacheImage
    private fun cacheImage(imageUrl: String?)
    {
        // No need to load, if we already have it
        if (imageCache.containsKey(imageUrl)){
            return;
        }

        // Load it
        val doLoadImagehRunnable: Runnable = Runnable {

            val url: URL = URL(imageUrl)

            // Create connection
            val con: HttpsURLConnection?
            try {
                con = url.openConnection() as HttpsURLConnection?
            } catch (e: IOException) {
                Log.d(this.javaClass.name, e.localizedMessage!!)
                e.printStackTrace()
                return@Runnable
            }

            // Set some paramaters
            con!!.setUseCaches(false)
            con.setDoInput(true)
            con.setConnectTimeout(30 * 1000) // 30 seconds to connect
            con.setReadTimeout(50 * 1000) // 5 seond timeout
            con.setRequestMethod("GET")

            try {
                con.connect()
                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, con.responseMessage)
                    return@Runnable
                }
                val baResponse = readFully(con.inputStream)
                val size = baResponse?.size
                if (size!! > 0) {
                    // Create bitmap drawable for use in UI
                    val bmd = getDrawableFromData(baResponse)
                    imageCache.put(imageUrl!!, bmd)

                    runOnUiThread(Runnable { viewAdapter!!.notifyDataSetChanged() })
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error: " + e.localizedMessage)
                return@Runnable
            } finally {
                con.disconnect()
            }
        }

        // Run thread
        Executors.newSingleThreadExecutor().execute(doLoadImagehRunnable)
    }

    fun getDrawableFromData(buffer: ByteArray ): BitmapDrawable {



        val b: Bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
        val bs: Bitmap = createScaledBitmap(b, 200, 250, false);

        val bd = BitmapDrawable(resources, bs)

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
            Log.d(TAG, "read error", e)
            null
        } catch (e: OutOfMemoryError) {
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

